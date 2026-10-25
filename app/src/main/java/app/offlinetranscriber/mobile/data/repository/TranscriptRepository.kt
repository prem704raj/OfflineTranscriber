package app.offlinetranscriber.mobile.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.room.withTransaction
import app.offlinetranscriber.mobile.R
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.database.TranscriptDao
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.domain.model.TranscriptionState
import app.offlinetranscriber.mobile.transcription.AudioProcessor
import app.offlinetranscriber.mobile.transcription.ModelManager
import app.offlinetranscriber.mobile.transcription.WhisperEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.domain.model.TranscriptionRequest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class DeleteAudioResult(
    val mediaDeleted: Boolean,
    val referenceCleared: Boolean,
    val message: String
)

class TranscriptRepository(
    private val context: Context,
    val modelManager: ModelManager,
    val whisperEngine: WhisperEngine,
    val audioProcessor: AudioProcessor,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val transcriptDao: TranscriptDao = database.transcriptDao()
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
    ): List<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity> = withContext(Dispatchers.IO) {
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

    suspend fun searchAndReplace(
        transcriptId: Long,
        findText: String,
        replaceText: String,
        matchCase: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (findText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Search text cannot be empty"))
        }

        val database = AppDatabase.getInstance(context)
        database.withTransaction {
            val transcript = transcriptDao.getTranscriptById(transcriptId)
                ?: return@withTransaction Result.failure(NoSuchElementException("Transcript not found: $transcriptId"))

            val segments = transcript.getSegments().toMutableList()
            val entities = transcriptDao.getSegmentsByTranscriptId(transcriptId)
            val entityMap = entities.associateBy { it.id }

            var totalReplacements = 0
            val updatedSegments = segments.mapIndexed { index, seg ->
                val count = if (matchCase) {
                    seg.text.split(findText).size - 1
                } else {
                    Regex(Regex.escape(findText), RegexOption.IGNORE_CASE).findAll(seg.text).count()
                }

                val matchedEntity = entityMap[seg.id] ?: entities.getOrNull(index)
                if (count > 0) {
                    totalReplacements += count
                    val quotedReplacement = java.util.regex.Matcher.quoteReplacement(replaceText)
                    val newText = if (matchCase) {
                        seg.text.replace(findText, replaceText)
                    } else {
                        seg.text.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), quotedReplacement)
                    }
                    if (matchedEntity != null) {
                        transcriptDao.updateSegmentEntity(matchedEntity.id, newText.trim(), matchedEntity.startMs, matchedEntity.endMs)
                    }
                    seg.copy(
                        id = matchedEntity?.id ?: seg.id,
                        text = newText.trim()
                    )
                } else {
                    if (matchedEntity != null && matchedEntity.id != seg.id) {
                        seg.copy(id = matchedEntity.id)
                    } else {
                        seg
                    }
                }
            }

            if (totalReplacements > 0) {
                val updatedJson = Json.encodeToString(updatedSegments)
                val updatedFullText = updatedSegments.joinToString(" ") { it.text.trim() }
                transcriptDao.updateSegments(transcriptId, updatedJson, updatedFullText)
            }

            Result.success(totalReplacements)
        }
    }

    suspend fun deleteAudioRetainingTranscript(transcriptId: Long): Result<DeleteAudioResult> = withContext(Dispatchers.IO) {
        val transcript = transcriptDao.getTranscriptById(transcriptId)
            ?: return@withContext Result.failure(NoSuchElementException("Transcript not found: $transcriptId"))

        var fileDeleted = false
        val uriStr = transcript.audioUriString
        if (!uriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriStr)
                when (uri.scheme) {
                    "file" -> {
                        val file = File(uri.path.orEmpty())
                        if (file.exists()) {
                            fileDeleted = file.delete()
                        }
                    }
                    "content" -> {
                        fileDeleted = try {
                            if (DocumentsContract.isDocumentUri(context, uri)) {
                                DocumentsContract.deleteDocument(context.contentResolver, uri)
                            } else {
                                context.contentResolver.delete(uri, null, null) > 0
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not delete content URI $uri", e)
                            false
                        }
                    }
                    else -> {
                        if (uriStr.startsWith("/")) {
                            val file = File(uriStr)
                            if (file.exists()) {
                                fileDeleted = file.delete()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error attempting to delete audio file: $uriStr", e)
            }
        }

        transcriptDao.clearAudioUri(transcriptId)
        val msg = if (fileDeleted) {
            "Audio file deleted. Transcript preserved."
        } else {
            "Audio reference unlinked. Transcript preserved."
        }
        Result.success(DeleteAudioResult(mediaDeleted = fileDeleted, referenceCleared = true, message = msg))
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
                initialPrompt = request.initialPrompt,
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

            // 4. Save to Database within atomic Room transaction
            val (savedEntity, numberedSegments) = database.withTransaction {
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
                    app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity(
                        transcriptId = savedId,
                        startMs = seg.startMs,
                        endMs = seg.endMs,
                        text = seg.text.trim()
                    )
                }
                val insertedIds = transcriptDao.insertSegments(segmentEntities)
                val numbered = segments.mapIndexed { index, seg ->
                    val dbId = insertedIds.getOrNull(index) ?: (index + 1).toLong()
                    seg.copy(id = dbId)
                }
                val updatedJson = Json.encodeToString(numbered)
                transcriptDao.updateSegments(savedId, updatedJson, fullText)
                Pair(entity.copy(id = savedId, segmentsJson = updatedJson), numbered)
            }

            onStateChange(TranscriptionState.Success(fullText, numberedSegments))
            Result.success(savedEntity)

        } catch (e: CancellationException) {
            throw e
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
            val loadResult = whisperEngine.loadModel(modelFile)
            if (loadResult.isFailure) {
                val error = "Failed to load model: ${loadResult.exceptionOrNull()?.message}"
                onStateChange(TranscriptionState.Error(error))
                return@withContext Result.failure(loadResult.exceptionOrNull()!!)
            }

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

            val (savedEntity, numberedSegments) = database.withTransaction {
                val savedId = transcriptDao.insertTranscript(entity)
                val segmentEntities = segments.map { seg ->
                    app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity(
                        transcriptId = savedId,
                        startMs = seg.startMs,
                        endMs = seg.endMs,
                        text = seg.text.trim()
                    )
                }
                val insertedIds = transcriptDao.insertSegments(segmentEntities)
                val numbered = segments.mapIndexed { index, seg ->
                    val dbId = insertedIds.getOrNull(index) ?: (index + 1).toLong()
                    seg.copy(id = dbId)
                }
                val updatedJson = Json.encodeToString(numbered)
                transcriptDao.updateSegments(savedId, updatedJson, fullText)
                entity.copy(id = savedId, segmentsJson = updatedJson) to numbered
            }

            onStateChange(TranscriptionState.Success(fullText, numberedSegments))
            Result.success(savedEntity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Bundled sample transcription error", e)
            onStateChange(TranscriptionState.Error(e.localizedMessage ?: "Unknown error", e))
            Result.failure(e)
        }
    }
}
