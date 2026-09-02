package com.example.transcriber.queue

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.transcriber.data.database.TranscriptDao
import com.example.transcriber.data.model.MediaType
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.transcription.AudioProcessor
import com.example.transcriber.transcription.WhisperEngine
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
    private val transcriptDao: TranscriptDao
) : TranscriptionExecutionGateway {

    companion object {
        private const val TAG = "AppTranscriptionGateway"
    }

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
            throw CancellationException("Transcription job ${request.jobId} cancelled before audio decoding.")
        }

        // 2. Decode Audio
        val inputUri = Uri.parse(request.inputUri)
        val sourceUri = Uri.parse(request.sourceUri)

        val audioSamples: FloatArray
        val durationMs: Long

        if (request.inputUri.startsWith("asset://")) {
            val assetName = request.inputUri.removePrefix("asset://")
            val inputStream = context.assets.open(assetName)
            val decodeResult = audioProcessor.decodeFromInputStream(inputStream)
            if (decodeResult.isFailure) {
                error("Failed to decode sample asset: ${decodeResult.exceptionOrNull()?.message}")
            }
            audioSamples = decodeResult.getOrThrow()
            durationMs = (audioSamples.size / 16L).coerceAtLeast(1L)
        } else {
            val metadata = audioProcessor.getAudioMetadata(inputUri)
            durationMs = metadata?.durationMs ?: 0L

            val decodeResult = audioProcessor.decodeAudioTo16kHzMono(inputUri) { _ ->
                // Decoding progress
            }
            if (decodeResult.isFailure) {
                error("Failed to decode audio: ${decodeResult.exceptionOrNull()?.message}")
            }
            audioSamples = decodeResult.getOrThrow()
        }

        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled before Whisper inference.")
        }

        // 3. Transcribe Audio
        val liveSegments = mutableListOf<TranscriptSegment>()
        val language = if (request.languageCode.isBlank() || request.languageCode.equals("auto", ignoreCase = true)) {
            "auto"
        } else {
            request.languageCode
        }

        val transcribeResult = whisperEngine.transcribe(
            audioSamples = audioSamples,
            language = language,
            translate = false,
            onProgress = { progressPercent ->
                // Native whisper callback progress
            },
            onNewSegment = { newSegment ->
                liveSegments.add(newSegment)
                val totalEst = if (durationMs > 0L) durationMs else (audioSamples.size / 16L).coerceAtLeast(1L)
                val progress = ((newSegment.endMs.toFloat() / totalEst.toFloat()) * 100f).toInt().coerceIn(0, 99)
                // Use a non-blocking launch or runBlocking inside callback context
                kotlinx.coroutines.runBlocking {
                    onProgress(progress)
                }
            }
        )

        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled during Whisper inference.")
        }

        if (transcribeResult.isFailure) {
            val error = transcribeResult.exceptionOrNull()?.message ?: "Transcription failed"
            error(error)
        }

        val segments = transcribeResult.getOrThrow()
        val fullText = segments.joinToString(" ") { it.text.trim() }

        val mediaType = when (request.sourceType) {
            TranscriptionSourceType.VIDEO -> MediaType.VIDEO
            else -> MediaType.AUDIO
        }

        val modelName = modelFile.nameWithoutExtension

        // 4. Save to Database
        val entity = TranscriptEntity.fromSegments(
            title = request.displayName,
            audioFileName = request.displayName,
            audioDurationMs = if (durationMs > 0L) durationMs else (audioSamples.size / 16L),
            segments = segments,
            modelUsed = modelName,
            audioUriString = request.inputUri,
            sourceUri = request.sourceUri,
            mediaType = mediaType
        )

        if (isCancelled()) {
            throw CancellationException("Transcription job ${request.jobId} cancelled before saving results.")
        }

        val savedId = transcriptDao.insertTranscript(entity)
        val segmentEntities = segments.map { seg ->
            TranscriptSegmentEntity(
                transcriptId = savedId,
                startMs = seg.startMs,
                endMs = seg.endMs,
                text = seg.text.trim()
            )
        }
        val insertedIds = transcriptDao.insertSegments(segmentEntities)
        val numberedSegments = segments.mapIndexed { index, seg ->
            val dbId = insertedIds.getOrNull(index) ?: (index + 1).toLong()
            seg.copy(id = dbId)
        }
        val updatedJson = Json.encodeToString(numberedSegments)
        transcriptDao.updateSegments(savedId, updatedJson, fullText)

        Log.i(TAG, "Transcription job ${request.jobId} successfully completed and saved as transcript $savedId")
        savedId
    }

    override fun cancelCurrent() {
        whisperEngine.cancelTranscription()
    }
}
