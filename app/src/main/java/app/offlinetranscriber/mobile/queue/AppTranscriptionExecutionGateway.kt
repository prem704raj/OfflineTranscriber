package app.offlinetranscriber.mobile.queue

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.performance.CapacityResult
import app.offlinetranscriber.mobile.performance.StorageCapacityChecker
import app.offlinetranscriber.mobile.transcription.AudioProcessor
import app.offlinetranscriber.mobile.transcription.ChunkSegmentMerger
import app.offlinetranscriber.mobile.transcription.Pcm16ChunkReader
import app.offlinetranscriber.mobile.transcription.WhisperEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AppTranscriptionExecutionGateway(
    private val context: Context,
    private val whisperEngine: WhisperEngine,
    private val audioProcessor: AudioProcessor,
    private val database: AppDatabase,
    private val queueRepository: TranscriptionQueueRepository
) : TranscriptionExecutionGateway {

    companion object {
        private const val TAG = "AppTranscriptionGateway"
        private const val WINDOW_SECONDS = 5 * 60
        private const val OVERLAP_SECONDS = 2
    }

    private val storageCapacityChecker = StorageCapacityChecker(context)
    private val chunkReader = Pcm16ChunkReader(
        windowSeconds = WINDOW_SECONDS,
        overlapSeconds = OVERLAP_SECONDS
    )
    private val segmentMerger = ChunkSegmentMerger(
        overlapMs = OVERLAP_SECONDS * 1000L
    )

    override suspend fun execute(
        request: ExecutionRequest,
        onProgress: suspend (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Long = withContext(Dispatchers.IO) {
        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled before start.")
        }

        val modelFile = File(request.modelPath)
        require(modelFile.exists()) {
            "Model file does not exist: ${request.modelPath}"
        }

        // 1. Ensure Model is Loaded
        val loadResult = whisperEngine.loadModel(modelFile)
        if (loadResult.isFailure) {
            val error = loadResult.exceptionOrNull()?.message ?: "Failed to load model"
            error(error)
        }

        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled before audio decode.")
        }

        val job = queueRepository.getById(request.jobId)

        // 2. Prepare or Resume PCM16 File
        val pcmDir = File(context.cacheDir, "transcription_pcm").apply { mkdirs() }
        val pcmFile = if (!job?.preparedPcmPath.isNullOrBlank()) {
            File(job!!.preparedPcmPath!!)
        } else {
            File(pcmDir, "job_${request.jobId}.pcm")
        }

        if (!pcmFile.exists() || pcmFile.length() == 0L) {
            // Check storage headroom before decoding
            val requiredBytes = 100L * 1024L * 1024L // ~100MB headroom
            when (storageCapacityChecker.checkInternal(requiredBytes)) {
                is CapacityResult.NotEnough -> {
                    error("Not enough disk space to decode audio. Please free at least 100 MB.")
                }
                CapacityResult.Enough -> Unit
            }

            val inputUri = Uri.parse(request.inputUri)
            audioProcessor.prepareToPcm16(
                uri = inputUri,
                outputFile = pcmFile,
                onProgress = { prepPercent ->
                    // Scaling audio prep to 0-10% of total job progress
                    val mapped = (prepPercent * 0.1f).toInt().coerceIn(0, 10)
                    onProgress(mapped)
                },
                isCancelled = isCancelled
            )
            queueRepository.setPreparedPcmPath(request.jobId, pcmFile.absolutePath)
        }

        val totalSamples = chunkReader.totalSamples(pcmFile)
        require(totalSamples > 0L) { "Prepared audio contains no PCM samples." }
        val totalDurationMs = totalSamples * 1000L / AudioProcessor.TARGET_SAMPLE_RATE

        // 3. Checkpoint Resume
        val accumulatedSegments = mutableListOf<TranscriptSegment>()
        var currentStartSample = 0L

        if (job != null && job.checkpointSample > 0L && job.partialSegmentsJson.isNotBlank() && job.partialSegmentsJson != "[]") {
            try {
                val restored = Json.decodeFromString<List<TranscriptSegment>>(job.partialSegmentsJson)
                accumulatedSegments.addAll(restored)
                currentStartSample = job.checkpointSample
                Log.i(TAG, "Resuming job ${request.jobId} from sample $currentStartSample with ${restored.size} segments.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore checkpoint for job ${request.jobId}, starting from beginning", e)
                accumulatedSegments.clear()
                currentStartSample = 0L
            }
        }

        val language = if (request.languageCode.isBlank() || request.languageCode.equals("auto", ignoreCase = true)) {
            "auto"
        } else {
            request.languageCode
        }

        var chunkIndex = (currentStartSample / ((WINDOW_SECONDS - OVERLAP_SECONDS) * AudioProcessor.TARGET_SAMPLE_RATE.toLong())).toInt()

        // 4. Windowed Chunk Inference Loop
        while (currentStartSample < totalSamples) {
            if (isCancelled()) {
                throw CancellationException("Transcription job ${request.jobId} cancelled during processing.")
            }

            val window = chunkReader.read(pcmFile, currentStartSample, chunkIndex)
                ?: break

            val windowResult = whisperEngine.transcribe(
                audioSamples = window.samples,
                language = language,
                translate = false,
                onProgress = { _ -> },
                onNewSegment = { _ -> }
            )

            if (isCancelled()) {
                throw CancellationException("Transcription job ${request.jobId} cancelled during Whisper inference.")
            }

            if (windowResult.isFailure) {
                val error = windowResult.exceptionOrNull()?.message ?: "Whisper chunk inference failed"
                error(error)
            }

            val rawChunkSegments = windowResult.getOrThrow()
            segmentMerger.merge(
                accumulated = accumulatedSegments,
                chunkStartMs = window.startMs,
                chunkIndex = chunkIndex,
                chunkSegments = rawChunkSegments
            )

            currentStartSample = chunkReader.nextStart(window)
            chunkIndex++

            val progressPercent = ((currentStartSample.toFloat() / totalSamples.toFloat()) * 90f + 10f)
                .toInt().coerceIn(10, 99)
            onProgress(progressPercent)

            // Persist checkpoint to database
            val segmentsJson = Json.encodeToString(accumulatedSegments)
            queueRepository.saveCheckpoint(
                id = request.jobId,
                nextSample = currentStartSample,
                segmentsJson = segmentsJson
            )
        }

        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled before final database commit.")
        }

        // 5. Atomic Room Persistence Transaction
        val fullText = accumulatedSegments.joinToString(" ") { it.text.trim() }
        val mediaType = when (request.sourceType) {
            TranscriptionSourceType.VIDEO -> MediaType.VIDEO
            else -> MediaType.AUDIO
        }
        val modelName = modelFile.nameWithoutExtension

        val transcriptDao = database.transcriptDao()
        val savedTranscriptId = database.withTransaction {
            val entity = TranscriptEntity.fromSegments(
                title = request.displayName,
                audioFileName = request.displayName,
                audioDurationMs = totalDurationMs,
                segments = accumulatedSegments,
                modelUsed = modelName,
                audioUriString = request.inputUri,
                sourceUri = request.sourceUri,
                mediaType = mediaType
            )

            val newId = transcriptDao.insertTranscript(entity)
            val segmentEntities = accumulatedSegments.map { seg ->
                TranscriptSegmentEntity(
                    transcriptId = newId,
                    startMs = seg.startMs,
                    endMs = seg.endMs,
                    text = seg.text.trim()
                )
            }
            val insertedSegmentIds = transcriptDao.insertSegments(segmentEntities)
            val numbered = accumulatedSegments.mapIndexed { i, seg ->
                val segId = insertedSegmentIds.getOrNull(i) ?: (i + 1).toLong()
                seg.copy(id = segId)
            }
            transcriptDao.updateSegments(newId, Json.encodeToString(numbered), fullText)
            queueRepository.clearCheckpoint(request.jobId)
            newId
        }

        // 6. Clean up temporary PCM file
        runCatching { pcmFile.delete() }

        Log.i(TAG, "Transcription job ${request.jobId} completed successfully -> transcript ID $savedTranscriptId")
        savedTranscriptId
    }

    override fun cancelCurrent() {
        whisperEngine.cancelTranscription()
    }

    override suspend fun discardCheckpoint(jobId: Long) {
        runCatching {
            val pcmFile = File(context.cacheDir, "transcription_pcm/job_$jobId.pcm")
            if (pcmFile.exists()) pcmFile.delete()
            queueRepository.clearCheckpoint(jobId)
        }
    }
}
