package com.example.transcriber.caption

import com.example.transcriber.caption.export.CaptionOutputDimensions
import com.example.transcriber.caption.model.CaptionExportResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionOutputDimensionsTest {

    @Test
    fun `original resolution keeps even dimensions without scaling`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 1920,
            sourceHeight = 1080,
            resolution = CaptionExportResolution.ORIGINAL
        )
        assertEquals(1920, dims.width)
        assertEquals(1080, dims.height)
    }

    @Test
    fun `original resolution rounds odd dimensions to even`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 1081,
            sourceHeight = 721,
            resolution = CaptionExportResolution.ORIGINAL
        )
        assertEquals(1080, dims.width)
        assertEquals(720, dims.height)
    }

    @Test
    fun `scales 4K down to 1080p landscape preserving 16 by 9 aspect ratio`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 3840,
            sourceHeight = 2160,
            resolution = CaptionExportResolution.P1080
        )
        assertEquals(1920, dims.width)
        assertEquals(1080, dims.height)
    }

    @Test
    fun `scales 4K down to 1080p portrait preserving 9 by 16 aspect ratio`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 2160,
            sourceHeight = 3840,
            resolution = CaptionExportResolution.P1080
        )
        assertEquals(1080, dims.width)
        assertEquals(1920, dims.height)
    }

    @Test
    fun `scales 1080p down to 720p`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 1920,
            sourceHeight = 1080,
            resolution = CaptionExportResolution.P720
        )
        assertEquals(1280, dims.width)
        assertEquals(720, dims.height)
    }

    @Test
    fun `does not upscale lower resolution source`() {
        val dims = CaptionOutputDimensions.calculate(
            sourceWidth = 640,
            sourceHeight = 480,
            resolution = CaptionExportResolution.P1080
        )
        assertEquals(640, dims.width)
        assertEquals(480, dims.height)
    }
}
