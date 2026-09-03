package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment

class MeetingChunker(
    private val targetChars: Int = 7_500,
    private val maxChars: Int = 9_000
) {

    fun chunk(
        segments: List<MeetingSourceSegment>
    ): List<MeetingChunk> {
        if (segments.isEmpty()) return emptyList()

        val result = mutableListOf<MeetingChunk>()
        var current = mutableListOf<MeetingSourceSegment>()
        var chars = 0

        fun flush() {
            if (current.isEmpty()) return
            result += MeetingChunk(
                index = result.size,
                segments = current.toList()
            )
            current = mutableListOf()
            chars = 0
        }

        segments.forEach { segment ->
            val clean = segment.text
                .replace(Regex("""\s+"""), " ")
                .trim()
            val addition = clean.length + 40

            if (current.isNotEmpty() && (chars >= targetChars || chars + addition > maxChars)) {
                flush()
            }

            current += segment.copy(text = clean)
            chars += addition
        }

        flush()
        return result
    }
}
