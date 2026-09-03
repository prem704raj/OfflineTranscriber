package app.offlinetranscriber.mobile.backup.restore

import app.offlinetranscriber.mobile.backup.format.BackupManifest

data class BackupInspection(
    val sessionId: String,
    val encrypted: Boolean,
    val manifest: BackupManifest,
    val totalMediaBytes: Long,
    val validationWarnings: List<String>
)
