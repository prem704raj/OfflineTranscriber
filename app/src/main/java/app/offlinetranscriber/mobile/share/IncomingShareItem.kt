package app.offlinetranscriber.mobile.share

import android.net.Uri

enum class IncomingMediaKind {
    AUDIO,
    VIDEO
}

data class IncomingShareItem(
    val uri: Uri,
    val mimeType: String,
    val kind: IncomingMediaKind
)
