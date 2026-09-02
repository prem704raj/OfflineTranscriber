package com.example.transcriber.share

data class ShareImportResult(
    val enqueuedCount: Int,
    val videoProRejected: Int,
    val queueLimitRejected: Int,
    val unsupportedOrFailed: Int,
    val needsModel: Boolean
) {
    val totalRejected: Int
        get() = videoProRejected + queueLimitRejected + unsupportedOrFailed
}
