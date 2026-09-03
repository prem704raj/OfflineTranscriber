package app.offlinetranscriber.mobile.video

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import app.offlinetranscriber.mobile.performance.CapacityResult
import app.offlinetranscriber.mobile.performance.StorageCapacityChecker
import java.io.File
import java.util.UUID

@UnstableApi
class VideoAudioExtractor(
    private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())
    private val storageCapacityChecker = StorageCapacityChecker(context)
    private var transformer: Transformer? = null
    private var currentOutput: File? = null
    private var progressRunnable: Runnable? = null

    fun extract(
        inputVideoUri: Uri,
        onProgress: (Int?) -> Unit,
        onSuccess: (Uri) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cancel()

        val inputSize = querySize(inputVideoUri) ?: (250L * 1024L * 1024L)
        val required = maxOf(
            150L * 1024L * 1024L,
            minOf(inputSize, 750L * 1024L * 1024L)
        )

        when (storageCapacityChecker.checkInternal(required)) {
            CapacityResult.Enough -> Unit
            is CapacityResult.NotEnough -> {
                onError(IllegalStateException("Not enough free storage to prepare this video."))
                return
            }
        }

        val outputDir = File(
            context.filesDir,
            "video_audio"
        ).apply { mkdirs() }

        val outputFile = File(
            outputDir,
            "audio_${UUID.randomUUID()}.mp4"
        )
        currentOutput = outputFile

        val edited = EditedMediaItem.Builder(
            MediaItem.fromUri(inputVideoUri)
        )
            .setRemoveVideo(true)
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(
                composition: Composition,
                result: ExportResult
            ) {
                stopProgressPolling()
                val file = currentOutput
                transformer = null
                currentOutput = null

                if (file == null || !file.exists() || file.length() <= 0L) {
                    onError(IllegalStateException("Audio extraction produced no output file."))
                    return
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    file
                )
                onProgress(100)
                onSuccess(uri)
            }

            override fun onError(
                composition: Composition,
                result: ExportResult,
                exception: ExportException
            ) {
                stopProgressPolling()
                currentOutput?.delete()
                currentOutput = null
                transformer = null
                onError(exception)
            }
        }

        val instance = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        transformer = instance

        try {
            instance.start(edited, outputFile.absolutePath)
            startProgressPolling(instance, onProgress)
        } catch (t: Throwable) {
            stopProgressPolling()
            outputFile.delete()
            transformer = null
            currentOutput = null
            onError(t)
        }
    }

    fun cancel() {
        stopProgressPolling()
        runCatching { transformer?.cancel() }
        transformer = null
        currentOutput?.delete()
        currentOutput = null
    }

    private fun querySize(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getLong(index).takeIf { it > 0L }
                } else null
            }
        }.getOrNull()
    }

    private fun startProgressPolling(
        instance: Transformer,
        onProgress: (Int?) -> Unit
    ) {
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                if (transformer !== instance) return

                val state = runCatching {
                    instance.getProgress(holder)
                }.getOrDefault(Transformer.PROGRESS_STATE_UNAVAILABLE)

                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress.coerceIn(0, 99))
                } else {
                    onProgress(null)
                }

                if (
                    state != Transformer.PROGRESS_STATE_NOT_STARTED &&
                    transformer === instance
                ) {
                    handler.postDelayed(this, 350L)
                }
            }
        }

        progressRunnable = runnable
        handler.post(runnable)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let(handler::removeCallbacks)
        progressRunnable = null
    }
}
