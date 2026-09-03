package app.offlinetranscriber.mobile.subtitle

import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import java.util.Locale

object SubtitleFormatter {
    fun toSrt(segments: List<TranscriptSegment>): String =
        segments.sortedBy { it.startMs }
            .mapIndexed { index, segment ->
                buildString {
                    append(index + 1).append('\n')
                    append(srtTime(segment.startMs))
                    append(" --> ")
                    append(srtTime(segment.endMs)).append('\n')
                    append(segment.text.trim()).append("\n\n")
                }
            }
            .joinToString("")

    fun toVtt(segments: List<TranscriptSegment>): String = buildString {
        append("WEBVTT\n\n")
        segments.sortedBy { it.startMs }.forEach { segment ->
            append(vttTime(segment.startMs))
            append(" --> ")
            append(vttTime(segment.endMs)).append('\n')
            append(segment.text.trim()).append("\n\n")
        }
    }

    private fun srtTime(ms: Long): String = formatTime(ms, ',')
    private fun vttTime(ms: Long): String = formatTime(ms, '.')

    private fun formatTime(ms: Long, separator: Char): String {
        val safe = ms.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val millis = safe % 1_000L
        return String.format(
            Locale.US,
            "%02d:%02d:%02d%c%03d",
            hours, minutes, seconds, separator, millis
        )
    }
}
