package app.offlinetranscriber.mobile.backup

import kotlinx.serialization.Serializable

@Serializable
enum class BackupOperationType {
    CREATE,
    INSPECT,
    RESTORE
}

@Serializable
enum class BackupOperationStatus {
    QUEUED,
    WAITING,
    RUNNING,
    READY_FOR_RESTORE,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class BackupOptions(
    val includeSettings: Boolean = true,
    val includeMedia: Boolean = false,
    val passwordProtected: Boolean = false
)

@Serializable
data class BackupProgress(
    val stage: String,
    val percent: Int
)

@Serializable
data class BackupOperationState(
    val operationId: String,
    val type: BackupOperationType,
    val status: BackupOperationStatus,
    val progress: Int = 0,
    val stage: String = "",
    val restoreSessionId: String? = null,
    val errorCode: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
enum class RestoreMode {
    MERGE,
    REPLACE
}

@Serializable
data class RestorePreview(
    val sessionId: String,
    val encrypted: Boolean,
    val createdAtEpochMs: Long,
    val appVersionName: String,
    val formatVersion: Int,
    val transcripts: Long,
    val segments: Long,
    val collections: Long,
    val studyPacks: Long,
    val askConversations: Long,
    val meetingPacks: Long,
    val speakerClusters: Long,
    val mediaCount: Int,
    val mediaBytes: Long,
    val includesSettings: Boolean,
    val warnings: List<String>
)

@Serializable
data class RestoreResult(
    val mode: RestoreMode,
    val importedTranscripts: Int,
    val reusedDuplicateTranscripts: Int,
    val importedSegments: Long,
    val restoredCollections: Int,
    val restoredStudyPacks: Int,
    val restoredAskConversations: Int,
    val restoredMeetingPacks: Int,
    val restoredSpeakerClusters: Int,
    val restoredMediaFiles: Int,
    val warnings: List<String>
)
