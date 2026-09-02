package com.example.transcriber.queue

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.example.transcriber.video.VideoAudioExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@UnstableApi
class VideoQueueInputPreparer(
    context: Context
) : QueueInputPreparer {

    private val extractor = VideoAudioExtractor(context.applicationContext)

    override suspend fun prepare(
        job: TranscriptionJobEntity,
        onProgress: suspend (Int) -> Unit,
        isCancelled: () -> Boolean
    ): PreparedQueueInput = coroutineScope {
        job.preparedInputUri
            ?.takeIf { it.isNotBlank() }
            ?.let {
                return@coroutineScope PreparedQueueInput(
                    audioUri = it,
                    preparationWeightPercent = if (job.sourceType == TranscriptionSourceType.VIDEO.name) 25 else 0
                )
            }

        val sourceType = runCatching {
            TranscriptionSourceType.valueOf(job.sourceType)
        }.getOrDefault(TranscriptionSourceType.AUDIO)

        if (sourceType != TranscriptionSourceType.VIDEO) {
            return@coroutineScope PreparedQueueInput(
                audioUri = job.inputUri,
                preparationWeightPercent = 0
            )
        }

        if (isCancelled()) {
            throw CancellationException("Cancelled")
        }

        val ownerScope = this

        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    extractor.cancel()
                }

                extractor.extract(
                    inputVideoUri = Uri.parse(job.inputUri),
                    onProgress = { rawPercent ->
                        val mapped = rawPercent
                            ?.coerceIn(0, 100)
                            ?.let { (it * 0.25f).toInt() }
                            ?: 5

                        ownerScope.launch {
                            onProgress(mapped)
                        }
                    },
                    onSuccess = { audioUri ->
                        if (continuation.isActive) {
                            continuation.resume(
                                PreparedQueueInput(
                                    audioUri = audioUri.toString(),
                                    preparationWeightPercent = 25
                                )
                            )
                        }
                    },
                    onError = { error ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                )
            }
        }
    }

    override fun cancelCurrentPreparation() {
        extractor.cancel()
    }
}
