package com.example.transcriber.queue

data class PreparedQueueInput(
    val audioUri: String,
    val preparationWeightPercent: Int
)

interface QueueInputPreparer {

    /**
     * Returns actual audio input for Whisper.
     *
     * Audio/recording jobs are no-op.
     * Raw video jobs use Media3 extraction.
     */
    suspend fun prepare(
        job: TranscriptionJobEntity,
        onProgress: suspend (Int) -> Unit,
        isCancelled: () -> Boolean
    ): PreparedQueueInput

    fun cancelCurrentPreparation()
}
