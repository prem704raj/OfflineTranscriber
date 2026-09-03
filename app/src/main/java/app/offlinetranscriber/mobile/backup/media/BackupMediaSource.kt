package app.offlinetranscriber.mobile.backup.media

import android.net.Uri

data class BackupMediaSource(
    val mediaId: String,
    val transcriptOldId: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val extension: String
)
