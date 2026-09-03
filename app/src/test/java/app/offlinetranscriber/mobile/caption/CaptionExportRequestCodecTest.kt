package app.offlinetranscriber.mobile.caption

import app.offlinetranscriber.mobile.caption.export.background.CaptionExportRequestCodec
import app.offlinetranscriber.mobile.caption.model.CaptionCue
import app.offlinetranscriber.mobile.caption.model.CaptionExportRequest
import app.offlinetranscriber.mobile.caption.model.CaptionExportResolution
import app.offlinetranscriber.mobile.caption.model.CaptionPreset
import app.offlinetranscriber.mobile.caption.style.CaptionPresetFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptionExportRequestCodecTest {

    @Test
    fun `encodes and decodes request accurately`() {
        val style = CaptionPresetFactory.style(CaptionPreset.BOX).copy(
            includeSpeakerLabel = true,
            speakerAccentEnabled = true
        )
        val request = CaptionExportRequest(
            transcriptId = 42L,
            sourceVideoUri = "content://media/external/video/media/123",
            style = style,
            resolution = CaptionExportResolution.P1080,
            outputFileName = "My Keynote - Captioned.mp4",
            cues = listOf(
                CaptionCue(
                    segmentId = 101L,
                    startUs = 1_000_000L,
                    endUs = 3_000_000L,
                    text = "Welcome to the presentation.",
                    speakerClusterId = 1L,
                    speakerLabel = "Alice",
                    speakerOrdinal = 0
                ),
                CaptionCue(
                    segmentId = 102L,
                    startUs = 3_500_000L,
                    endUs = 6_000_000L,
                    text = "Let's review our Q3 metrics.",
                    speakerClusterId = 2L,
                    speakerLabel = "Bob",
                    speakerOrdinal = 1
                )
            )
        )

        val json = CaptionExportRequestCodec.encode(request)
        val decoded = CaptionExportRequestCodec.decode(json)

        assertEquals(request.transcriptId, decoded.transcriptId)
        assertEquals(request.sourceVideoUri, decoded.sourceVideoUri)
        assertEquals(request.resolution, decoded.resolution)
        assertEquals(request.outputFileName, decoded.outputFileName)
        assertEquals(request.style.preset, decoded.style.preset)
        assertEquals(request.style.includeSpeakerLabel, decoded.style.includeSpeakerLabel)
        assertEquals(request.style.speakerAccentEnabled, decoded.style.speakerAccentEnabled)
        assertEquals(2, decoded.cues.size)
        assertEquals("Alice", decoded.cues[0].speakerLabel)
        assertEquals("Bob", decoded.cues[1].speakerLabel)
        assertEquals("Welcome to the presentation.", decoded.cues[0].text)
    }
}
