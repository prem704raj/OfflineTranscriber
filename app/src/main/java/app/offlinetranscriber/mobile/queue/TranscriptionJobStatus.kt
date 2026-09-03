package app.offlinetranscriber.mobile.queue

enum class TranscriptionJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
