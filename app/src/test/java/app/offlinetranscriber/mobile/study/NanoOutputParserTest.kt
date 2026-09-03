package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.study.nano.NanoOutputParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoOutputParserTest {

    @Test
    fun chapterTimestampSnapsToRealSegment() {
        val segments = listOf(
            segment(1, 0),
            segment(2, 10_000),
            segment(3, 20_000)
        )

        val parsed = NanoOutputParser.parse(
            raw = """
                KP|Normalization reduces redundancy.
                CH|11234|Normalization|This section explains normalization.
                FC|What is normalization?|A process used to organize data.
            """.trimIndent(),
            realSegments = segments
        )

        assertEquals(1, parsed.chapters.size)
        assertEquals(
            10_000L,
            parsed.chapters.first().startMs
        )
        assertEquals(1, parsed.flashcards.size)
        assertTrue(parsed.keyPoints.isNotEmpty())
    }

    private fun segment(
        id: Long,
        start: Long
    ) = TranscriptSegmentEntity(
        id = id,
        transcriptId = 1,
        startMs = start,
        endMs = start + 5_000,
        text = "Example"
    )
}
