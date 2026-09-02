package com.example.transcriber.backup.format

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val product: String,
    val formatVersion: Int,
    val backupId: String,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val appVersionCode: Long,
    val sourceRoomSchemaVersion: Int,
    val includesSettings: Boolean,
    val includesMedia: Boolean,
    val sectionCounts: Map<String, Long>,
    val entries: List<BackupManifestEntry>,
    val media: List<BackupMediaManifestEntry>,
    val warnings: List<String>
)

@Serializable
data class BackupManifestEntry(
    val name: String,
    val uncompressedBytes: Long,
    val sha256: String
)

@Serializable
data class BackupMediaManifestEntry(
    val mediaId: String,
    val transcriptOldId: Long,
    val role: String,
    val entryName: String,
    val displayName: String,
    val mimeType: String?,
    val uncompressedBytes: Long,
    val sha256: String
)
