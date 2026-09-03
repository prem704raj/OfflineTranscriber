package app.offlinetranscriber.mobile.caption

import app.offlinetranscriber.mobile.caption.model.CaptionCue
import app.offlinetranscriber.mobile.caption.source.CaptionCueValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionCueValidatorTest {

    @Test
    fun `filters out blank cues`() {
        val cues = listOf(
            CaptionCue(1L, 0L, 1_000_000L, "   "),
            CaptionCue(2L, 1_000_000L, 2_000_000L, "Valid"),
            CaptionCue(3L, 2_000_000L, 3_000_000L, "")
        )
        val valid = CaptionCueValidator.validate(cues, durationMs = 10_000L)
        assertEquals(1, valid.size)
        assertEquals(2L, valid[0].segmentId)
    }

    @Test
    fun `filters out inverted or zero duration cues`() {
        val cues = listOf(
            CaptionCue(1L, 2_000_000L, 1_000_000L, "Inverted"),
            CaptionCue(2L, 2_000_000L, 2_000_000L, "Zero"),
            CaptionCue(3L, 1_000_000L, 2_000_000L, "Good")
        )
        val valid = CaptionCueValidator.validate(cues, durationMs = 10_000L)
        assertEquals(1, valid.size)
        assertEquals(3L, valid[0].segmentId)
    }

    @Test
    fun `clamps cues to duration`() {
        val cues = listOf(
            CaptionCue(1L, 4_000_000L, 8_000_000L, "Exceeds duration")
        )
        val valid = CaptionCueValidator.validate(cues, durationMs = 5_000L)
        assertEquals(1, valid.size)
        assertEquals(4_000_000L, valid[0].startUs)
        assertEquals(5_000_000L, valid[0].endUs)
    }

    @Test
    fun `sorts cues by start time then end time`() {
        val cues = listOf(
            CaptionCue(2L, 3_000_000L, 4_000_000L, "Second"),
            CaptionCue(1L, 1_000_000L, 2_000_000L, "First")
        )
        val valid = CaptionCueValidator.validate(cues, durationMs = 10_000L)
        assertEquals(1L, valid[0].segmentId)
        assertEquals(2L, valid[1].segmentId)
    }
}
