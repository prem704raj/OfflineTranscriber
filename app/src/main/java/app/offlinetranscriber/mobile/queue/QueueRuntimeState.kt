package app.offlinetranscriber.mobile.queue

sealed interface QueueRuntimeState {

    data object Idle : QueueRuntimeState

    data class Running(
        val jobId: Long,
        val stage: TranscriptionJobStage,
        val progress: Int,
        val queuedAfterCurrent: Int
    ) : QueueRuntimeState
}
