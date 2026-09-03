package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity

data class TranscriptChunk(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val segmentIds: List<Long>
)

object TranscriptChunker {

    private const val TARGET_CHARS = 5_500
    private const val MAX_AI_CHUNKS = 8

    fun build(
        segments: List<TranscriptSegmentEntity>,
        maxChunks: Int = MAX_AI_CHUNKS
    ): List<TranscriptChunk> {
        if (segments.isEmpty()) return emptyList()

        val raw = mutableListOf<TranscriptChunk>()
        var current = mutableListOf<TranscriptSegmentEntity>()
        var chars = 0

        fun flush() {
            if (current.isEmpty()) return

            raw += TranscriptChunk(
                startMs = current.first().startMs,
                endMs = current.last().endMs,
                text = current.joinToString("\n") {
                    "[${it.startMs}] ${it.text.trim()}"
                },
                segmentIds = current.map { it.id }
            )
            current = mutableListOf()
            chars = 0
        }

        segments.sortedBy { it.startMs }.forEach { segment ->
            val lineLength = segment.text.length + 24

            if (
                current.isNotEmpty() &&
                chars + lineLength > TARGET_CHARS
            ) {
                flush()
            }

            current += segment
            chars += lineLength
        }

        flush()

        if (raw.size <= maxChunks) return raw

        // Even sampling preserves coverage of the full recording instead of
        // silently ignoring the ending of a long lecture.
        val selected = LinkedHashSet<Int>()
        for (i in 0 until maxChunks) {
            val fraction =
                if (maxChunks == 1) 0.0
                else i.toDouble() / (maxChunks - 1).toDouble()

            selected +=
                (fraction * (raw.lastIndex).toDouble())
                    .toInt()
                    .coerceIn(0, raw.lastIndex)
        }

        return selected
            .sorted()
            .map(raw::get)
    }
}
