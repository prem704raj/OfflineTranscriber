package app.offlinetranscriber.mobile.caption.export

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object SourceVideoInfoReader {

    fun read(
        context: Context,
        videoUri: Uri
    ): SourceVideoInfo {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1280
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 720
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

            return SourceVideoInfo(
                width = width,
                height = height,
                rotationDegrees = rotation,
                durationMs = duration,
                hasAudio = hasAudio
            )
        } finally {
            retriever.release()
        }
    }
}
