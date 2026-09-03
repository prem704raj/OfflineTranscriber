package app.offlinetranscriber.mobile.transcription

import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkSegmentMergerTest {

    @Test
    fun testFirstChunkShiftAndAccumulate() {
        val merger = ChunkSegmentMerger(overlapMs = 2_000L)
        val accumulated = mutableListOf<TranscriptSegment>()

        val chunk0 = listOf(
            TranscriptSegment(id = 0, startMs = 500, endMs = 2500, text = "Hello and welcome"),
            TranscriptSegment(id = 0, startMs = 2600, endMs = 4500, text = "to offline transcription.")
        )

        merger.merge(accumulated, chunkStartMs = 0L, chunkIndex = 0, chunkSegments = chunk0)

        assertEquals(2, accumulated.size)
        assertEquals(500L, accumulated[0].startMs)
        assertEquals(2500L, accumulated[0].endMs)
        assertEquals("Hello and welcome", accumulated[0].text)
        assertEquals(2600L, accumulated[1].startMs)
        assertEquals(4500L, accumulated[1].endMs)
    }

    @Test
    fun testSecondChunkTimestampShiftingAndDeduplication() {
        val merger = ChunkSegmentMerger(overlapMs = 2_000L)
        val accumulated = mutableListOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 2800, text = "First sentence here."),
            TranscriptSegment(id = 2, startMs = 2900, endMs = 4900, text = "Boundary sentence.")
        )

        // Chunk 1 starts at 3,000ms (overlap is 2,000ms, from 3,000 to 5,000ms)
        // Acceptance floor = 3000 + 1000 = 4000ms.
        val chunk1 = listOf(
            // Ends at 3000 + 800 = 3800ms <= 4000ms floor -> should be dropped
            TranscriptSegment(id = 0, startMs = 0, endMs = 800, text = "sentence here."),
            // Ends at 3000 + 1900 = 4900ms, text matches "Boundary sentence." -> duplicate, should be skipped
            TranscriptSegment(id = 0, startMs = 0, endMs = 1900, text = "Boundary sentence."),
            // Ends at 3000 + 3500 = 6500ms -> new segment, should be accepted and shifted
            TranscriptSegment(id = 0, startMs = 2000, endMs = 3500, text = "Third sentence follows.")
        )

        merger.merge(accumulated, chunkStartMs = 3000L, chunkIndex = 1, chunkSegments = chunk1)

        assertEquals(3, accumulated.size)
        assertEquals(5000L, accumulated[2].startMs)
        assertEquals(6500L, accumulated[2].endMs)
        assertEquals("Third sentence follows.", accumulated[2].text)
    }
}
