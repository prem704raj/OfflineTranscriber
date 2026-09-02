package com.example.transcriber.speaker.modelmanager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class SpeakerModelDownloadState {
    data object Idle : SpeakerModelDownloadState()
    data class Downloading(val currentModel: String, val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : SpeakerModelDownloadState()
    data class Extracting(val currentModel: String) : SpeakerModelDownloadState()
    data object Ready : SpeakerModelDownloadState()
    data class Error(val message: String) : SpeakerModelDownloadState()
}

class SpeakerModelManager(
    private val context: Context
) {
    private val modelDir: File = File(context.filesDir, "speaker_models")

    private val _downloadState = MutableStateFlow<SpeakerModelDownloadState>(
        if (areModelsReady()) SpeakerModelDownloadState.Ready else SpeakerModelDownloadState.Idle
    )
    val downloadState: StateFlow<SpeakerModelDownloadState> = _downloadState.asStateFlow()

    init {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
    }

    fun getSegmentationModelFile(): File {
        return File(modelDir, "sherpa-onnx-pyannote-segmentation-3-0/model.int8.onnx")
    }

    fun getEmbeddingModelFile(): File {
        return File(modelDir, SpeakerModelCatalog.EMBEDDING_MODEL.fileName)
    }

    fun areModelsReady(): Boolean {
        val segFile = getSegmentationModelFile()
        val embFile = getEmbeddingModelFile()
        return segFile.exists() && segFile.length() > 0 && embFile.exists() && embFile.length() > 0
    }

    suspend fun downloadModels(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (areModelsReady()) {
                _downloadState.value = SpeakerModelDownloadState.Ready
                return@withContext Result.success(Unit)
            }

            modelDir.mkdirs()

            // 1. Download Segmentation Model Archive
            val segSpec = SpeakerModelCatalog.SEGMENTATION_MODEL
            val segTarget = getSegmentationModelFile()
            if (!segTarget.exists() || segTarget.length() == 0L) {
                val archiveFile = File(modelDir, segSpec.fileName)
                downloadFileWithProgress(segSpec, archiveFile)
                _downloadState.value = SpeakerModelDownloadState.Extracting(segSpec.label)
                extractTarBz2(archiveFile, modelDir)
                archiveFile.delete()
            }

            // 2. Download Embedding Model
            val embSpec = SpeakerModelCatalog.EMBEDDING_MODEL
            val embTarget = getEmbeddingModelFile()
            if (!embTarget.exists() || embTarget.length() == 0L) {
                downloadFileWithProgress(embSpec, embTarget)
            }

            if (!areModelsReady()) {
                val errorMsg = "Model verification failed after download"
                _downloadState.value = SpeakerModelDownloadState.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            _downloadState.value = SpeakerModelDownloadState.Ready
            Result.success(Unit)
        } catch (e: Exception) {
            _downloadState.value = SpeakerModelDownloadState.Error(e.message ?: "Failed to download speaker models")
            Result.failure(e)
        }
    }

    private suspend fun downloadFileWithProgress(
        spec: SpeakerModelSpec,
        destination: File
    ) {
        val tempFile = File(destination.parentFile, "${destination.name}.download")
        if (tempFile.exists()) tempFile.delete()

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(spec.downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode from ${spec.downloadUrl}")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: spec.sizeBytes
            var bytesReadTotal = 0L

            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesReadTotal += bytesRead
                val progress = if (totalBytes > 0) bytesReadTotal.toFloat() / totalBytes else 0.5f
                _downloadState.value = SpeakerModelDownloadState.Downloading(
                    currentModel = spec.label,
                    progress = progress.coerceIn(0f, 1f),
                    bytesDownloaded = bytesReadTotal,
                    totalBytes = totalBytes
                )
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Verify SHA256 if specified
            if (spec.expectedSha256.isNotBlank()) {
                val calculatedSha256 = calculateSha256(tempFile)
                if (!calculatedSha256.equals(spec.expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    throw IllegalStateException("SHA-256 mismatch for ${spec.fileName}. Expected: ${spec.expectedSha256}, got: $calculatedSha256")
                }
            }

            if (destination.exists()) destination.delete()
            if (!tempFile.renameTo(destination)) {
                tempFile.copyTo(destination, overwrite = true)
                tempFile.delete()
            }
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    private fun extractTarBz2(archiveFile: File, targetDir: File) {
        FileInputStream(archiveFile).use { fis ->
            BZip2CompressorInputStream(fis).use { bzIn ->
                TarArchiveInputStream(bzIn).use { tarIn ->
                    var entry = tarIn.nextEntry
                    while (entry != null) {
                        val outputFile = File(targetDir, entry.name)
                        // Protection against ZipSlip
                        if (!outputFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                            throw SecurityException("Illegal archive entry path: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            FileOutputStream(outputFile).use { fos ->
                                tarIn.copyTo(fos)
                            }
                        }
                        entry = tarIn.nextEntry
                    }
                }
            }
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16384)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun deleteModels() {
        modelDir.deleteRecursively()
        modelDir.mkdirs()
        _downloadState.value = SpeakerModelDownloadState.Idle
    }
}
