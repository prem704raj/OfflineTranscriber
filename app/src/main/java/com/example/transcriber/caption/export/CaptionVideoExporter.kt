package com.example.transcriber.caption.export

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionExportResolution
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.render.CaptionEffectFactory
import com.example.transcriber.caption.render.CaptionStyleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CaptionVideoExportResult(
    val file: File,
    val durationMs: Long
)

@OptIn(UnstableApi::class)
class CaptionVideoExporter(
    private val context: Context
) {

    suspend fun export(
        sourceUri: Uri,
        cues: List<CaptionCue>,
        style: CaptionStyle,
        resolution: CaptionExportResolution,
        outputFile: File,
        outputDimensions: OutputDimensions?,
        onProgress: suspend (Int) -> Unit,
        cancelled: () -> Boolean
    ): CaptionVideoExportResult =
        withContext(Dispatchers.Main.immediate) {
            val fixedStyle = CaptionStyleProvider(style)
            val videoEffects = mutableListOf<Effect>()

            if (outputDimensions != null) {
                videoEffects += Presentation.createForWidthAndHeight(
                    outputDimensions.width,
                    outputDimensions.height,
                    Presentation.LAYOUT_SCALE_TO_FIT
                )
            }

            videoEffects += CaptionEffectFactory.create(
                cues = cues,
                styleProvider = fixedStyle
            )

            val mediaItem = MediaItem.fromUri(sourceUri)
            val edited = EditedMediaItem.Builder(mediaItem)
                .setEffects(
                    Effects(
                        /* audioProcessors = */ emptyList(),
                        /* videoEffects = */ videoEffects
                    )
                )
                .build()

            suspendCancellableCoroutine { continuation ->
                var progressJob: Job? = null

                val listener = object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult
                    ) {
                        progressJob?.cancel()
                        if (continuation.isActive) {
                            continuation.resume(
                                CaptionVideoExportResult(
                                    file = outputFile,
                                    durationMs = exportResult.durationMs
                                )
                            )
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        progressJob?.cancel()
                        if (continuation.isActive) {
                            continuation.resumeWithException(exportException)
                        }
                    }
                }

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()

                val holder = ProgressHolder()

                progressJob = CoroutineScope(
                    Dispatchers.Main.immediate + SupervisorJob()
                ).launch {
                    while (isActive && continuation.isActive) {
                        if (cancelled()) {
                            transformer.cancel()
                            continuation.cancel()
                            break
                        }

                        when (transformer.getProgress(holder)) {
                            Transformer.PROGRESS_STATE_AVAILABLE -> {
                                onProgress(holder.progress.coerceIn(0, 99))
                            }
                            else -> Unit
                        }

                        delay(350L)
                    }
                }

                continuation.invokeOnCancellation {
                    progressJob?.cancel()
                    transformer.cancel()
                }

                transformer.start(edited, outputFile.absolutePath)
            }
        }
}
