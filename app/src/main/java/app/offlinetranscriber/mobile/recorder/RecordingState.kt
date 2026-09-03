package app.offlinetranscriber.mobile.recorder

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class RecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val durationMs: Long = 0L,
    val amplitude: Float = 0f,
    val filePath: String? = null,
    val errorMessage: String? = null
)
