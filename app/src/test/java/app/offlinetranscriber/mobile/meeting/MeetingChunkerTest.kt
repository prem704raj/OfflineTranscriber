package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingChunkerTest {

    @Test
    fun preservesAllSegmentsAndOrder() {
        val segments = (1..10).map { i ->
            MeetingSourceSegment(
                segmentId = i.toLong(),
                transcriptId = 1L,
                startMs = i * 1000L,
                endMs = (i + 1) * 1000L,
                text = "This is segment number $i with some discussion text."
            )
        }

        val chunker = MeetingChunker(targetChars = 100, maxChars = 200)
        val chunks = chunker.chunk(segments)

        assertTrue(chunks.size > 1)

        val flattened = chunks.flatMap { it.segments }
        assertEquals(segments.size, flattened.size)
        assertEquals(segments.map { it.segmentId }, flattened.map { it.segmentId })
    }

    @Test
    fun emptyInputReturnsEmpty() {
        val chunker = MeetingChunker()
        assertTrue(chunker.chunk(emptyList()).isEmpty())
    }
}
