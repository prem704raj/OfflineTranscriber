package com.example.transcriber.queue

enum class TranscriptionJobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
