package com.example.transcriber.caption

import com.example.transcriber.caption.edit.CaptionReadability
import com.example.transcriber.caption.edit.CaptionReadabilityAnalyzer
import com.example.transcriber.caption.model.CaptionCue
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionReadabilityAnalyzerTest {

    @Test
    fun `short text is good`() {
        val cue = CaptionCue(1L, 0L, 2_000_000L, "Hello world this is a test.")
        val result = CaptionReadabilityAnalyzer.analyze(cue, maxLines = 2)
        assertTrue(result.contains(CaptionReadability.GOOD))
    }

    @Test
    fun `very long text exceeding capacity is flagged`() {
        val longText = "This is a very long sentence that goes on and on and contains way too many words to comfortably fit on two lines of video captioning without obstructing half the frame."
        val cue = CaptionCue(1L, 0L, 5_000_000L, longText)
        val result = CaptionReadabilityAnalyzer.analyze(cue, maxLines = 2)
        assertTrue(result.contains(CaptionReadability.VERY_LONG))
    }

    @Test
    fun `fast speech over 24 characters per second is flagged as FAST`() {
        val fastText = "This sentence has thirty-nine chars!" // 36 chars in 1s = 36 cps
        val cue = CaptionCue(1L, 0L, 1_000_000L, fastText)
        val result = CaptionReadabilityAnalyzer.analyze(cue, maxLines = 2)
        assertTrue(result.contains(CaptionReadability.FAST))
    }
}
