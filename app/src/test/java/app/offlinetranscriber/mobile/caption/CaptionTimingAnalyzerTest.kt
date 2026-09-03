package app.offlinetranscriber.mobile.caption

import app.offlinetranscriber.mobile.caption.edit.CaptionTimingAnalyzer
import app.offlinetranscriber.mobile.caption.model.CaptionCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionTimingAnalyzerTest {

    @Test
    fun `flags short captions below 300ms`() {
        val cues = listOf(
            CaptionCue(1L, 0L, 200_000L, "Too fast"),
            CaptionCue(2L, 500_000L, 2_000_000L, "Normal length")
        )
        val issues = CaptionTimingAnalyzer.analyze(cues)
        assertEquals(1, issues.size)
        assertEquals(1L, issues[0].segmentId)
        assertTrue(issues[0].message.contains("shorter than 0.3 seconds"))
    }

    @Test
    fun `flags overlapping captions above 250ms`() {
        val cues = listOf(
            CaptionCue(1L, 0L, 2_000_000L, "First block"),
            CaptionCue(2L, 1_500_000L, 3_000_000L, "Overlaps by 500ms")
        )
        val issues = CaptionTimingAnalyzer.analyze(cues)
        assertEquals(1, issues.size)
        assertEquals(1L, issues[0].segmentId)
        assertTrue(issues[0].message.contains("overlaps"))
    }

    @Test
    fun `does not flag small acceptable overlaps under 250ms`() {
        val cues = listOf(
            CaptionCue(1L, 0L, 2_000_000L, "First block"),
            CaptionCue(2L, 1_900_000L, 3_000_000L, "Overlaps by 100ms")
        )
        val issues = CaptionTimingAnalyzer.analyze(cues)
        assertTrue(issues.isEmpty())
    }
}
