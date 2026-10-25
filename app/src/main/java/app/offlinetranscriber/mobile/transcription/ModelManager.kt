package app.offlinetranscriber.mobile.transcription

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class WhisperModelInfo(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val description: String,
    val isMultilingual: Boolean,
    val isRecommended: Boolean = false
) {
    val sizeFormatted: String
        get() = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
}

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState
    data class Completed(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

class ModelManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("transcriber_models", Context.MODE_PRIVATE)

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    companion object {
        private const val TAG = "ModelManager"
        private const val PREF_SELECTED_MODEL_ID = "selected_model_id"

        private data class ModelIntegrity(val sha256: String, val minimumBytes: Long)

        private val LEGACY_MODEL_INTEGRITY = mapOf(
            "ggml-tiny.en.bin" to ModelIntegrity(
                "921e4cf8686fdd993dcd081a5da5b6c365bfde1162e72b08d75ac75289920b1f",
                40L * 1024 * 1024
            ),
            "ggml-tiny.bin" to ModelIntegrity(
                "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21",
                40L * 1024 * 1024
            ),
            "ggml-base.bin" to ModelIntegrity(
                "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
                70L * 1024 * 1024
            ),
            "ggml-base.en.bin" to ModelIntegrity(
                "a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002",
                70L * 1024 * 1024
            ),
            "ggml-base-q5_1.bin" to ModelIntegrity(
                "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
                30L * 1024 * 1024
            ),
            "ggml-small.bin" to ModelIntegrity(
                "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b",
                250L * 1024 * 1024
            )
        )

        val AVAILABLE_MODELS = listOf(
            WhisperModelInfo(
                id = "base-q5_1",
                name = "Base Q5.1 (Quantized)",
                fileName = "ggml-base-q5_1.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
                sizeBytes = 57_400_000L,
                description = "Optimal balance of memory and accuracy. Best for standard Android devices.",
                isMultilingual = true,
                isRecommended = true
            ),
            WhisperModelInfo(
                id = "tiny-en",
                name = "Tiny (English only)",
                fileName = "ggml-tiny.en.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
                sizeBytes = 75_000_000L,
                description = "Fastest transcription with ultra-low memory footprint for English.",
                isMultilingual = false
            ),
            WhisperModelInfo(
                id = "tiny",
                name = "Tiny (Multilingual)",
                fileName = "ggml-tiny.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
                sizeBytes = 75_000_000L,
                description = "Fast multilingual transcription with low memory usage.",
                isMultilingual = true
            ),
            WhisperModelInfo(
                id = "base",
                name = "Base (Multilingual)",
                fileName = "ggml-base.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
                sizeBytes = 142_000_000L,
                description = "Full Base model with high transcription accuracy across 99+ languages.",
                isMultilingual = true
            ),
            WhisperModelInfo(
                id = "base-en",
                name = "Base (English only)",
                fileName = "ggml-base.en.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en.bin",
                sizeBytes = 142_000_000L,
                description = "Enhanced accuracy for English audio.",
                isMultilingual = false
            ),
            WhisperModelInfo(
                id = "small",
                name = "Small (Multilingual)",
                fileName = "ggml-small.bin",
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
                sizeBytes = 466_000_000L,
                description = "Highest accuracy for complex lectures and accents (requires 2GB+ free RAM).",
                isMultilingual = true
            )
        )
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates = _downloadStates.asStateFlow()

    private val verifiedCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    private fun modelCandidates(model: WhisperModelInfo): List<File> = listOf(
        File(File(context.filesDir, "whisper_models"), model.fileName),
        File(modelsDir, model.fileName)
    ).distinctBy { it.absolutePath }

    private fun integrityFor(model: WhisperModelInfo): ModelIntegrity? =
        app.offlinetranscriber.mobile.modelmanager.ModelCatalog.byFileName(model.fileName)
            ?.let { ModelIntegrity(it.sha256, it.minimumValidBytes) }
            ?: LEGACY_MODEL_INTEGRITY[model.fileName]

    private fun verifyFile(file: File, model: WhisperModelInfo): Boolean {
        if (!file.exists() || !file.isFile) return false
        val integrity = integrityFor(model)
        if (integrity == null) {
            return file.length() >= maxOf(10_000_000L, model.sizeBytes / 2)
        }
        if (file.length() < integrity.minimumBytes) return false
        val cacheKey = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        return verifiedCache.computeIfAbsent(cacheKey) {
            app.offlinetranscriber.mobile.security.Sha256.matches(file, integrity.sha256)
        }
    }

    fun getModelFile(model: WhisperModelInfo): File {
        val candidates = modelCandidates(model)
        return candidates.firstOrNull { verifyFile(it, model) } ?: candidates.first()
    }

    fun isModelDownloaded(model: WhisperModelInfo): Boolean =
        modelCandidates(model).any { verifyFile(it, model) }

    fun getDownloadedModels(): List<WhisperModelInfo> {
        return AVAILABLE_MODELS.filter { isModelDownloaded(it) }
    }

    fun getSelectedModel(): WhisperModelInfo {
        val savedId = prefs.getString(PREF_SELECTED_MODEL_ID, null)
        val found = AVAILABLE_MODELS.firstOrNull { it.id == savedId }
        if (found != null && isModelDownloaded(found)) {
            return found
        }
        // Fallback to first downloaded model or default recommended model
        return getDownloadedModels().firstOrNull() ?: AVAILABLE_MODELS.first { it.isRecommended }
    }

    fun setSelectedModel(model: WhisperModelInfo) {
        prefs.edit().putString(PREF_SELECTED_MODEL_ID, model.id).apply()
    }

    fun deleteModel(model: WhisperModelInfo): Boolean {
        var deleted = false
        modelCandidates(model).forEach { file ->
            if (file.exists()) {
                verifiedCache.keys.removeAll { it.startsWith("${file.absolutePath}:") }
                deleted = file.delete() || deleted
            }
        }
        return deleted
    }

    suspend fun importModelFromUri(uri: Uri, customName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val destinationFile = File(modelsDir, "custom_${System.currentTimeMillis()}.bin")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for Uri"))

            Result.success(destinationFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom model", e)
            Result.failure(e)
        }
    }

    fun downloadModel(model: WhisperModelInfo): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0, 0, model.sizeBytes))
        updateState(model.id, DownloadState.Downloading(0, 0, model.sizeBytes))

        val targetFile = getModelFile(model)
        val tempFile = File(modelsDir, "${model.fileName}.tmp")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(model.downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode !in 200..299) {
                val err = "Download failed with HTTP code: ${connection.responseCode}"
                emit(DownloadState.Error(err))
                updateState(model.id, DownloadState.Error(err))
                return@flow
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else model.sizeBytes
            var bytesDownloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var lastReportTime = System.currentTimeMillis()

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 250 || bytesDownloaded == totalBytes) {
                            lastReportTime = now
                            val progress = ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            val state = DownloadState.Downloading(progress, bytesDownloaded, totalBytes)
                            emit(state)
                            updateState(model.id, state)
                        }
                    }
                }
            }

            val integrity = integrityFor(model)
            if (integrity != null) {
                val valid = tempFile.length() >= integrity.minimumBytes &&
                    app.offlinetranscriber.mobile.security.Sha256.matches(tempFile, integrity.sha256)
                if (!valid) {
                    tempFile.delete()
                    val err = "Downloaded model failed SHA-256 integrity verification"
                    Log.e(TAG, err)
                    emit(DownloadState.Error(err))
                    updateState(model.id, DownloadState.Error(err))
                    return@flow
                }
            }

            targetFile.parentFile?.mkdirs()
            if (targetFile.exists() && !verifyFile(targetFile, model)) {
                targetFile.delete()
            }
            if (targetFile.exists() && verifyFile(targetFile, model)) {
                tempFile.delete()
                val completed = DownloadState.Completed(targetFile)
                emit(completed)
                updateState(model.id, completed)
                setSelectedModel(model)
            } else if (tempFile.renameTo(targetFile)) {
                verifiedCache.keys.removeAll { it.startsWith("${targetFile.absolutePath}:") }
                val completed = DownloadState.Completed(targetFile)
                emit(completed)
                updateState(model.id, completed)
                setSelectedModel(model)
            } else {
                tempFile.delete()
                val err = "Failed to finalize downloaded file"
                emit(DownloadState.Error(err))
                updateState(model.id, DownloadState.Error(err))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download error for ${model.name}", e)
            tempFile.delete()
            val err = e.localizedMessage ?: "Unknown download error"
            emit(DownloadState.Error(err))
            updateState(model.id, DownloadState.Error(err))
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun updateState(modelId: String, state: DownloadState) {
        val current = _downloadStates.value.toMutableMap()
        current[modelId] = state
        _downloadStates.value = current
    }
}
