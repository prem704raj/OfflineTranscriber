package app.offlinetranscriber.mobile.transcription

import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import java.util.Locale
import kotlin.math.abs

class ChunkSegmentMerger(private val overlapMs: Long = 2_000L) {

    fun merge(
        accumulated: MutableList<TranscriptSegment>,
        chunkStartMs: Long,
        chunkIndex: Int,
        chunkSegments: List<TranscriptSegment>
    ) {
        val acceptanceFloor =
            if (chunkIndex == 0) Long.MIN_VALUE
            else chunkStartMs + overlapMs / 2L

        for (segment in chunkSegments) {
            val shifted = segment.copy(
                startMs = segment.startMs + chunkStartMs,
                endMs = segment.endMs + chunkStartMs
            )

            if (chunkIndex > 0 && shifted.endMs <= acceptanceFloor) continue

            val last = accumulated.lastOrNull()
            if (last != null && isDuplicate(last, shifted)) continue

            accumulated += shifted
        }
    }

    private fun isDuplicate(previous: TranscriptSegment, next: TranscriptSegment): Boolean {
        val nearBoundary =
            abs(previous.startMs - next.startMs) < 3_000L ||
                abs(previous.endMs - next.endMs) < 3_000L

        if (!nearBoundary) return false
        return normalized(previous.text) == normalized(next.text)
    }

    private fun normalized(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}
