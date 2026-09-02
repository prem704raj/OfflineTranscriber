package com.example.transcriber.caption.export.background

import kotlinx.serialization.Serializable

@Serializable
enum class CaptionExportJobStatus {
    QUEUED,
    WAITING,
    EXPORTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class CaptionExportJobState(
    val jobId: String,
    val transcriptId: Long,
    val status: CaptionExportJobStatus,
    val progress: Int = 0,
    val readyFilePath: String? = null,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
