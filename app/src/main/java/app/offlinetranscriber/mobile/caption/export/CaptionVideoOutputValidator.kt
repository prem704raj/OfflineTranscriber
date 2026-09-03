package app.offlinetranscriber.mobile.caption.export

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File

object CaptionVideoOutputValidator {

    fun validate(
        context: Context,
        file: File
    ) {
        require(file.isFile && file.length() > 1024L) {
            "Captioned video was not created or is empty."
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0

            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0

            require(duration > 0L && width > 0 && height > 0) {
                "Captioned video is invalid (duration=$duration, dimensions=${width}x$height)."
            }
        } finally {
            retriever.release()
        }
    }
}
