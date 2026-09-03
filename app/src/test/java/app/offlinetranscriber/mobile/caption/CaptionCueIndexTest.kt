package app.offlinetranscriber.mobile.caption

import app.offlinetranscriber.mobile.caption.model.CaptionCue
import app.offlinetranscriber.mobile.caption.render.CaptionCueIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionCueIndexTest {

    @Test
    fun `empty cues returns null for any timestamp`() {
        val index = CaptionCueIndex(emptyList())
        assertNull(index.active(0L))
        assertNull(index.active(5_000_000L))
    }

    @Test
    fun `finds correct active cue within range`() {
        val cues = listOf(
            CaptionCue(1L, 0L, 2_000_000L, "First"),
            CaptionCue(2L, 2_000_000L, 4_000_000L, "Second"),
            CaptionCue(3L, 5_000_000L, 8_000_000L, "Third")
        )
        val index = CaptionCueIndex(cues)

        assertEquals(1L, index.active(0L)?.segmentId)
        assertEquals(1L, index.active(1_000_000L)?.segmentId)
        assertEquals(1L, index.active(1_999_999L)?.segmentId)
        assertEquals(2L, index.active(2_000_000L)?.segmentId)
        assertEquals(2L, index.active(3_999_999L)?.segmentId)

        // Gap between 4s and 5s
        assertNull(index.active(4_500_000L))

        assertEquals(3L, index.active(5_000_000L)?.segmentId)
        assertEquals(3L, index.active(7_999_999L)?.segmentId)
        assertNull(index.active(8_000_000L))
    }

    @Test
    fun `resolves overlapping cues by preferring latest start`() {
        val cues = listOf(
            CaptionCue(1L, 1_000_000L, 5_000_000L, "Long"),
            CaptionCue(2L, 2_000_000L, 4_000_000L, "Nested")
        )
        val index = CaptionCueIndex(cues)

        assertEquals(1L, index.active(1_500_000L)?.segmentId)
        assertEquals(2L, index.active(2_500_000L)?.segmentId)
        assertEquals(1L, index.active(4_500_000L)?.segmentId)
    }
}
