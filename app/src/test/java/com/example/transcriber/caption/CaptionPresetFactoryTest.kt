package com.example.transcriber.caption

import com.example.transcriber.caption.model.CaptionPreset
import com.example.transcriber.caption.style.CaptionPresetFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionPresetFactoryTest {

    @Test
    fun `preset styles have expected configurations`() {
        val classic = CaptionPresetFactory.style(CaptionPreset.CLASSIC)
        assertEquals(CaptionPreset.CLASSIC, classic.preset)
        assertFalse(classic.backgroundEnabled)
        assertTrue(classic.outlineWidthFactor > 0f)

        val clean = CaptionPresetFactory.style(CaptionPreset.CLEAN)
        assertEquals(CaptionPreset.CLEAN, clean.preset)
        assertTrue(clean.backgroundEnabled)
        assertEquals(0f, clean.outlineWidthFactor)

        val bold = CaptionPresetFactory.style(CaptionPreset.BOLD)
        assertEquals(CaptionPreset.BOLD, bold.preset)
        assertTrue(bold.outlineWidthFactor >= 0.05f)

        val box = CaptionPresetFactory.style(CaptionPreset.BOX)
        assertEquals(CaptionPreset.BOX, box.preset)
        assertTrue(box.backgroundEnabled)

        val minimal = CaptionPresetFactory.style(CaptionPreset.MINIMAL)
        assertEquals(CaptionPreset.MINIMAL, minimal.preset)
        assertFalse(minimal.backgroundEnabled)
    }
}
