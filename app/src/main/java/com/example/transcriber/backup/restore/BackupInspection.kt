package com.example.transcriber.backup.restore

import com.example.transcriber.backup.format.BackupManifest

data class BackupInspection(
    val sessionId: String,
    val encrypted: Boolean,
    val manifest: BackupManifest,
    val totalMediaBytes: Long,
    val validationWarnings: List<String>
)
