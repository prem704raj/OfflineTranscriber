package app.offlinetranscriber.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val id: Long = 0L
) {
    val formattedTimestamp: String
        get() = "${formatTime(startMs)} → ${formatTime(endMs)}"

    val startTimestampShort: String
        get() = formatTime(startMs)

    companion object {
        fun formatTime(timeMs: Long): String {
            val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds)
            } else {
                String.format("%02d:%02d", remainingMinutes, seconds)
            }
        }
    }
}

sealed interface TranscriptionState {
    object Idle : TranscriptionState
    data class DecodingAudio(val progress: Float = 0f) : TranscriptionState
    data class Transcribing(
        val progress: Int,
        val partialSegments: List<TranscriptSegment> = emptyList(),
        val currentSpeedRatio: Float = 1.0f
    ) : TranscriptionState
    data class Success(val fullText: String, val segments: List<TranscriptSegment>) : TranscriptionState
    data class Error(val message: String, val throwable: Throwable? = null) : TranscriptionState
}
