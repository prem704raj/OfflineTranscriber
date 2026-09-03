package app.offlinetranscriber.mobile.meeting

object MeetingSourceFormatter {

    data class LabeledChunk(
        val text: String,
        val labelToSegment: Map<String, Long>
    )

    fun format(
        chunk: MeetingChunk
    ): LabeledChunk {
        val map = LinkedHashMap<String, Long>()

        val text = buildString {
            chunk.segments.forEachIndexed { index, segment ->
                val label = "S${index + 1}"
                map[label] = segment.segmentId

                append("[")
                append(label)
                append("] ")
                append(segment.startMs)
                append("ms")
                if (!segment.speakerName.isNullOrBlank() && segment.speakerUserNamed) {
                    append(" | Speaker: ")
                    append(segment.speakerName)
                }
                append(" | ")
                appendLine(
                    segment.text
                        .replace("\n", " ")
                        .take(900)
                )
            }
        }

        return LabeledChunk(
            text = text,
            labelToSegment = map
        )
    }
}
