package com.example.transcriber.queue

data class ExecutionRequest(
    val jobId: Long,
    val inputUri: String,
    val sourceUri: String,
    val sourceType: TranscriptionSourceType,
    val displayName: String,
    val modelPath: String,
    val languageCode: String
)

interface TranscriptionExecutionGateway {

    /**
     * Executes transcription delegating to the single Whisper transcription pipeline.
     *
     * Returns the newly saved TranscriptEntity ID.
     */
    suspend fun execute(
        request: ExecutionRequest,
        onProgress: suspend (Int) -> Unit,
        isCancelled: () -> Boolean
    ): Long

    /**
     * Forwards cancellation request to the active transcription engine.
     */
    fun cancelCurrent()
}
