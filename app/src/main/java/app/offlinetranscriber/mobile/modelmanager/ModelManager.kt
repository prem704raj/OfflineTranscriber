package app.offlinetranscriber.mobile.modelmanager

import android.content.Context
import android.util.Log
import app.offlinetranscriber.mobile.performance.CapacityResult
import app.offlinetranscriber.mobile.performance.StorageCapacityChecker
import app.offlinetranscriber.mobile.settings.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelManager(
    private val context: Context,
    private val settings: AppSettingsRepository
) {
    companion object {
        private const val TAG = "ModelManager"
    }

    private val storageCapacityChecker = StorageCapacityChecker(context)

    private val _downloadState =
        MutableStateFlow<ModelDownloadState>(
            ModelDownloadState.Idle
        )
    val downloadState = _downloadState.asStateFlow()

    @Volatile
    private var cancelRequested = false

    private val verifiedModels = java.util.concurrent.ConcurrentHashMap<String, String>()

    val modelsDir: File
        get() = File(
            context.filesDir,
            "whisper_models"
        ).apply { mkdirs() }

    fun modelFile(
        spec: WhisperModelSpec
    ): File = File(
        modelsDir,
        spec.fileName
    )

    fun isInstalled(
        spec: WhisperModelSpec
    ): Boolean {
        val file = modelFile(spec)
        return file.exists() &&
            file.isFile &&
            file.length() >= spec.minimumValidBytes
    }

    fun verifySha(spec: WhisperModelSpec): Boolean {
        val file = modelFile(spec)
        if (!isInstalled(spec)) return false
        val cacheKey = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        val cached = verifiedModels[cacheKey]
        if (cached != null) {
            return cached.equals(spec.sha256, ignoreCase = true)
        }
        val computed = app.offlinetranscriber.mobile.security.Sha256.of(file)
        verifiedModels[cacheKey] = computed
        return computed.equals(spec.sha256, ignoreCase = true)
    }

    fun installedModels(): List<WhisperModelSpec> =
        ModelCatalog.all.filter(::isInstalled)

    suspend fun selectedOrBestInstalled(): WhisperModelSpec? {
        val selectedId =
            settings.settings.first()
                .selectedModelId

        val selected =
            ModelCatalog.byId(selectedId)

        if (
            selected != null &&
            isInstalled(selected)
        ) {
            return selected
        }

        val installed = installedModels()
        val fallback =
            installed.firstOrNull {
                it.id == ModelCatalog.balanced.id
            } ?: installed.firstOrNull()

        if (fallback != null) {
            settings.setSelectedModel(fallback.id)
        }

        return fallback
    }

    suspend fun select(
        spec: WhisperModelSpec
    ) {
        require(isInstalled(spec)) {
            "Model is not installed."
        }
        settings.setSelectedModel(spec.id)
    }

    suspend fun delete(
        spec: WhisperModelSpec
    ) {
        val current =
            settings.settings.first()
                .selectedModelId

        if (current == spec.id) {
            val alternate =
                installedModels()
                    .firstOrNull {
                        it.id != spec.id
                    }

            require(alternate != null) {
                "Download another model before deleting the active model."
            }

            settings.setSelectedModel(
                alternate.id
            )
        }

        val final = modelFile(spec)
        val part = File(
            final.parentFile,
            final.name + ".part"
        )

        part.delete()
        final.delete()
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    suspend fun download(
        spec: WhisperModelSpec
    ) = withContext(Dispatchers.IO) {
        cancelRequested = false

        val finalFile = modelFile(spec)

        if (isInstalled(spec)) {
            _downloadState.value =
                ModelDownloadState.Completed(
                    spec.id
                )
            return@withContext
        }

        val partFile = File(
            finalFile.parentFile,
            finalFile.name + ".part"
        )

        val existing = if (partFile.exists()) partFile.length() else 0L
        val remaining = maxOf(spec.approximateBytes - existing, 0L)

        when (storageCapacityChecker.checkInternal(remaining)) {
            CapacityResult.Enough -> Unit
            is CapacityResult.NotEnough -> {
                _downloadState.value = ModelDownloadState.Failed(
                    modelId = spec.id,
                    message = "Not enough storage. Free some space and try again."
                )
                return@withContext
            }
        }

        try {
            val connection =
                URL(spec.downloadUrl)
                    .openConnection()
                    as HttpURLConnection

            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true

            if (existing > 0L) {
                connection.setRequestProperty(
                    "Range",
                    "bytes=$existing-"
                )
            }

            connection.connect()

            val response = connection.responseCode

            val canResume =
                response == HttpURLConnection.HTTP_PARTIAL

            val startBytes =
                if (canResume) existing
                else 0L

            if (
                response !in listOf(
                    HttpURLConnection.HTTP_OK,
                    HttpURLConnection.HTTP_PARTIAL
                )
            ) {
                error(
                    "Model download failed: HTTP $response"
                )
            }

            if (!canResume && partFile.exists()) {
                partFile.delete()
            }

            val bodyLength =
                connection.contentLengthLong
                    .takeIf { it > 0L }

            val total =
                bodyLength?.let {
                    startBytes + it
                }

            BufferedInputStream(
                connection.inputStream
            ).use { input ->
                FileOutputStream(
                    partFile,
                    canResume
                ).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = startBytes

                    while (true) {
                        if (cancelRequested) {
                            throw DownloadCancelled()
                        }

                        val read = input.read(buffer)
                        if (read < 0) break

                        output.write(buffer, 0, read)
                        downloaded += read

                        _downloadState.value =
                            ModelDownloadState.Downloading(
                                modelId = spec.id,
                                bytesDownloaded = downloaded,
                                totalBytes = total
                            )
                    }

                    output.fd.sync()
                }
            }

            connection.disconnect()

            if (partFile.length() < spec.minimumValidBytes) {
                partFile.delete()
                error("Downloaded model file is incomplete.")
            }

            if (!app.offlinetranscriber.mobile.security.Sha256.matches(partFile, spec.sha256)) {
                partFile.delete()
                error("Model verification failed. Please download it again.")
            }

            if (finalFile.exists()) {
                finalFile.delete()
            }

            if (!partFile.renameTo(finalFile)) {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
            }

            if (!isInstalled(spec)) {
                finalFile.delete()
                error("Model validation failed.")
            }

            val verifiedKey = "${finalFile.absolutePath}:${finalFile.length()}:${finalFile.lastModified()}"
            verifiedModels[verifiedKey] = spec.sha256

            if (
                settings.settings.first()
                    .selectedModelId
                    .isBlank()
            ) {
                settings.setSelectedModel(spec.id)
            }

            _downloadState.value =
                ModelDownloadState.Completed(
                    spec.id
                )
        } catch (_: DownloadCancelled) {
            _downloadState.value =
                ModelDownloadState.Idle
            // keep .part for resumable future download
        } catch (t: Throwable) {
            _downloadState.value =
                ModelDownloadState.Failed(
                    modelId = spec.id,
                    message =
                        t.message
                            ?: "Model download failed."
                )
        }
    }

    fun clearTransientState() {
        _downloadState.value =
            ModelDownloadState.Idle
    }

    fun totalInstalledBytes(): Long =
        installedModels()
            .sumOf {
                modelFile(it).length()
            }

    /**
     * Adopts models previously downloaded in older app versions (e.g. files/models/)
     * into the canonical files/whisper_models/ directory if valid.
     */
    suspend fun adoptExistingModels() = withContext(Dispatchers.IO) {
        try {
            val oldDir = File(context.filesDir, "models")
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.listFiles()?.forEach { oldFile ->
                    val spec = ModelCatalog.byFileName(oldFile.name)
                    if (spec != null && oldFile.length() >= spec.minimumValidBytes) {
                        val target = modelFile(spec)
                        if (!target.exists() || target.length() < spec.minimumValidBytes) {
                            oldFile.copyTo(target, overwrite = true)
                            Log.i(TAG, "Adopted existing model: ${spec.fileName}")
                        }
                    }
                }
            }
            // Ensure a valid model is selected if none currently chosen
            selectedOrBestInstalled()
        } catch (e: Exception) {
            Log.w(TAG, "Error adopting existing models", e)
        }
    }

    private class DownloadCancelled : RuntimeException()
}
