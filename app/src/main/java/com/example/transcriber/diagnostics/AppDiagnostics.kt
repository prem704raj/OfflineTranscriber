package com.example.transcriber.diagnostics

data class QueueDiagnostics(
    val queued: Int,
    val processing: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int
)

data class AppDiagnostics(
    val appVersionName: String,
    val appVersionCode: Long,
    val androidRelease: String,
    val apiLevel: Int,
    val totalRamBytes: Long,
    val cpuCores: Int,
    val activeModelLabel: String,
    val installedModelLabels: List<String>,
    val languageLabel: String,
    val databaseBytes: Long,
    val transcriptCount: Int,
    val segmentCount: Int,
    val bookmarkCount: Int,
    val collectionCount: Int,
    val studyPackCount: Int,
    val askConversationCount: Int,
    val askMessageCount: Int,
    val meetingPackCount: Int,
    val meetingActionCount: Int,
    val speakerClusterCount: Int = 0,
    val speakerTurnCount: Int = 0,
    val diarizedTranscriptCount: Int = 0,
    val queue: QueueDiagnostics,
    val recordingsBytes: Long,
    val extractedAudioBytes: Long,
    val temporaryBytes: Long,
    val modelsBytes: Long,
    val freeInternalBytes: Long,
    val entitlement: String,
    val nanoStatus: String
)
