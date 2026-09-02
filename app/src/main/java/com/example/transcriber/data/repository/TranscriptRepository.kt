package com.example.transcriber.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.transcriber.R
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.data.database.TranscriptDao
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.domain.model.TranscriptionState
import com.example.transcriber.transcription.AudioProcessor
import com.example.transcriber.transcription.ModelManager
import com.example.transcriber.transcription.WhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import com.example.transcriber.data.model.MediaType
import com.example.transcriber.domain.model.TranscriptionRequest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TranscriptRepository(
    private val context: Context,
    val modelManager: ModelManager,
    val whisperEngine: WhisperEngine,
    val audioProcessor: AudioProcessor,
    private val transcriptDao: TranscriptDao = AppDatabase.getInstance(context).transcriptDao()
) {
    companion object {
        private const val TAG = "TranscriptRepository"
    }

    val allTranscripts: Flow<List<TranscriptEntity>> = transcriptDao.getAllTranscripts()

    suspend fun getTranscriptById(id: Long): TranscriptEntity? = withContext(Dispatchers.IO) {
        transcriptDao.getTranscriptById(id)
    }

    fun observeTranscript(id: Long): Flow<TranscriptEntity?> = transcriptDao.observeTranscript(id)

    fun observeSegments(id: Long): Flow<List<TranscriptSegment>> =
        transcriptDao.observeTranscript(id).map { entity ->
            entity?.getSegments().orEmpty()
        }

    suspend fun getSegmentsOnce(
        transcriptId: Long
    ): List<com.example.transcriber.data.model.TranscriptSegmentEntity> = withContext(Dispatchers.IO) {
        transcriptDao.getSegmentsOnce(transcriptId)
    }

    suspend fun updateSegment(
        transcriptId: Long,
        segmentId: Long,
        text: String,
        startMs: Long,
        endMs: Long
    ) = withContext(Dispatchers.IO) {
        val transcript = transcriptDao.getTranscriptById(transcriptId) ?: return@withContext
        val segments = transcript.getSegments().toMutableList()
        val index = segments.indexOfFirst { it.id == segmentId }
        if (index >= 0) {
            segments[index] = segments[index].copy(
                text = text.trim(),
                startMs = startMs,
                endMs = endMs
            )
            val updatedJson = Json.encodeToString(segments)
            val updatedFullText = segments.joinToString(" ") { it.text.trim() }
            transcriptDao.updateSegments(transcriptId, updatedJson, updatedFullText)
            transcriptDao.updateSegmentEntity(segmentId, text.trim(), startMs, endMs)
        }
    }

    suspend fun updateTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        transcriptDao.updateTitle(id, newTitle)
    }

    suspend fun deleteTranscript(id: Long) = withContext(Dispatchers.IO) {
        transcriptDao.deleteById(id)
    }

    fun cancelActiveTranscription() {
        whisperEngine.cancelTranscription()
    }

    suspend fun transcribe(
        request: TranscriptionRequest,
        onStateChange: (TranscriptionState) -> Unit
    ): Result<TranscriptEntity> = withContext(Dispatchers.IO) {
        try {
            // 1. Ensure Model is ready
            val selectedModel = modelManager.getSelectedModel()
            if (!modelManager.isModelDownloaded(selectedModel)) {
                val error = "Model ${selectedModel.name} is not downloaded yet. Please download it first."
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(IllegalStateException(error))
            }

            val modelFile = modelManager.getModelFile(selectedModel)
            val loadResult = whisperEngine.loadModel(modelFile)
            if (loadResult.isFailure) {
                val error = "Failed to load model: ${loadResult.exceptionOrNull()?.message}"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(loadResult.exceptionOrNull()!!)
            }

            // 2. Decode Audio from request.audioUri
            onStateChange(TranscriptionState.DecodingAudio(0f))
            val audioMetadata = audioProcessor.getAudioMetadata(request.audioUri)
            val durationMs = audioMetadata?.durationMs ?: 0L

            val decodeResult = audioProcessor.decodeAudioTo16kHzMono(request.audioUri) { progress ->
                onStateChange(TranscriptionState.DecodingAudio(progress))
            }

            if (decodeResult.isFailure) {
                val error = "Failed to decode audio: ${decodeResult.exceptionOrNull()?.message}"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(decodeResult.exceptionOrNull()!!)
            }

            val audioSamples = decodeResult.getOrThrow()

            // 3. Transcribe Audio
            val liveSegments = mutableListOf<TranscriptSegment>()
            onStateChange(TranscriptionState.Transcribing(0, liveSegments))

            val transcribeResult = whisperEngine.transcribe(
                audioSamples = audioSamples,
                language = request.language,
                translate = request.translate,
                onProgress = { progress ->
                    onStateChange(TranscriptionState.Transcribing(progress, liveSegments.toList()))
                },
                onNewSegment = { newSegment ->
                    liveSegments.add(newSegment)
                    onStateChange(TranscriptionState.Transcribing(
                        progress = (newSegment.endMs.toFloat() / durationMs.coerceAtLeast(1) * 100).toInt().coerceIn(0, 99),
                        partialSegments = liveSegments.toList()
                    ))
                }
            )

            if (transcribeResult.isFailure) {
                val error = transcribeResult.exceptionOrNull()?.message ?: "Transcription failed"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(transcribeResult.exceptionOrNull()!!)
            }

            val segments = transcribeResult.getOrThrow()
            val fullText = segments.joinToString(" ") { it.text.trim() }

            val fileName = request.audioFileName ?: (request.suggestedTitle ?: "media")
            val title = request.suggestedTitle?.ifBlank { fileName.substringBeforeLast('.') }
                ?: fileName.substringBeforeLast('.')

            // 4. Save to Database
            val entity = TranscriptEntity.fromSegments(
                title = title,
                audioFileName = fileName,
                audioDurationMs = durationMs.takeIf { it > 0 } ?: (audioSamples.size / 16L),
                segments = segments,
                modelUsed = selectedModel.name,
                audioUriString = request.audioUri.toString(),
                sourceUri = request.sourceUri.toString(),
                mediaType = request.mediaType
            )

            val savedId = transcriptDao.insertTranscript(entity)
            val segmentEntities = segments.map { seg ->
                com.example.transcriber.data.model.TranscriptSegmentEntity(
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

            val savedEntity = entity.copy(id = savedId, segmentsJson = updatedJson)

            onStateChange(TranscriptionState.Success(fullText, numberedSegments))
            Result.success(savedEntity)

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            onStateChange(TranscriptionState.Error(e.localizedMessage ?: "Unknown error", e))
            Result.failure(e)
        }
    }

    suspend fun transcribeAudio(
        uri: Uri,
        title: String,
        audioFileName: String,
        language: String = "auto",
        translate: Boolean = false,
        onStateChange: (TranscriptionState) -> Unit
    ): Result<TranscriptEntity> = transcribe(
        request = TranscriptionRequest(
            audioUri = uri,
            sourceUri = uri,
            mediaType = MediaType.AUDIO,
            suggestedTitle = title,
            audioFileName = audioFileName,
            language = language,
            translate = translate
        ),
        onStateChange = onStateChange
    )

    suspend fun transcribeBundledSample(
        onStateChange: (TranscriptionState) -> Unit
    ): Result<TranscriptEntity> = withContext(Dispatchers.IO) {
        try {
            val selectedModel = modelManager.getSelectedModel()
            if (!modelManager.isModelDownloaded(selectedModel)) {
                val error = "Please download a Whisper model (e.g. Base Q5.1 or Tiny) before running sample test."
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(IllegalStateException(error))
            }

            val modelFile = modelManager.getModelFile(selectedModel)
            whisperEngine.loadModel(modelFile)

            onStateChange(TranscriptionState.DecodingAudio(0.5f))
            val inputStream = context.assets.open("jfk.wav")
            val decodeResult = audioProcessor.decodeFromInputStream(inputStream)
            if (decodeResult.isFailure) {
                val error = "Failed to load sample audio: ${decodeResult.exceptionOrNull()?.message}"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(decodeResult.exceptionOrNull()!!)
            }

            val audioSamples = decodeResult.getOrThrow()
            val liveSegments = mutableListOf<TranscriptSegment>()
            onStateChange(TranscriptionState.Transcribing(0, liveSegments))

            val transcribeResult = whisperEngine.transcribe(
                audioSamples = audioSamples,
                language = "en",
                translate = false,
                onProgress = { progress ->
                    onStateChange(TranscriptionState.Transcribing(progress, liveSegments.toList()))
                },
                onNewSegment = { newSegment ->
                    liveSegments.add(newSegment)
                    onStateChange(TranscriptionState.Transcribing(
                        progress = (newSegment.endMs.toFloat() / 11000f * 100).toInt().coerceIn(0, 99),
                        partialSegments = liveSegments.toList()
                    ))
                }
            )

            if (transcribeResult.isFailure) {
                val error = transcribeResult.exceptionOrNull()?.message ?: "Transcription failed"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(transcribeResult.exceptionOrNull()!!)
            }

            val segments = transcribeResult.getOrThrow()
            val fullText = segments.joinToString(" ") { it.text.trim() }

            val entity = TranscriptEntity.fromSegments(
                title = "JFK Historic Inaugural Address Sample",
                audioFileName = "jfk.wav",
                audioDurationMs = 11000L,
                segments = segments,
                modelUsed = selectedModel.name,
                audioUriString = "asset://jfk.wav"
            )

            val savedId = transcriptDao.insertTranscript(entity)
            val segmentEntities = segments.map { seg ->
                com.example.transcriber.data.model.TranscriptSegmentEntity(
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

            val savedEntity = entity.copy(id = savedId, segmentsJson = updatedJson)

            onStateChange(TranscriptionState.Success(fullText, numberedSegments))
            Result.success(savedEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Bundled sample transcription error", e)
            onStateChange(TranscriptionState.Error(e.localizedMessage ?: "Unknown error", e))
            Result.failure(e)
        }
    }
}
