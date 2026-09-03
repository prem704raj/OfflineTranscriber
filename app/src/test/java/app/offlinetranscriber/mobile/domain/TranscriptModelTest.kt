package app.offlinetranscriber.mobile.domain

import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.subtitle.SubtitleFormatter
import app.offlinetranscriber.mobile.subtitle.SubtitleTimingValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptModelTest {

    @Test
    fun testTimeFormatting_secondsAndMinutes() {
        assertEquals("00:00", TranscriptSegment.formatTime(0))
        assertEquals("00:05", TranscriptSegment.formatTime(5000))
        assertEquals("01:23", TranscriptSegment.formatTime(83000))
        assertEquals("54:31", TranscriptSegment.formatTime(3271000))
    }

    @Test
    fun testTimeFormatting_hours() {
        assertEquals("01:05:12", TranscriptSegment.formatTime(3912000))
    }

    @Test
    fun testTranscriptEntity_serialization_withVideoMetadata() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 0, endMs = 3200, text = "Good morning everyone."),
            TranscriptSegment(id = 2, startMs = 3200, endMs = 8500, text = "Today we will learn database normalization.")
        )

        val entity = TranscriptEntity.fromSegments(
            title = "Database Lecture",
            audioFileName = "lecture.mp4",
            audioDurationMs = 8500,
            segments = segments,
            modelUsed = "Base Q5.1 (Quantized)",
            audioUriString = "file:///data/audio.mp4",
            sourceUri = "content://media/external/video/123",
            mediaType = MediaType.VIDEO
        )

        assertEquals("Database Lecture", entity.title)
        assertEquals("Good morning everyone. Today we will learn database normalization.", entity.fullText)
        assertEquals("lecture.mp4", entity.audioFileName)
        assertEquals(MediaType.VIDEO, entity.mediaType)
        assertEquals("content://media/external/video/123", entity.sourceUri)

        val decodedSegments = entity.getSegments()
        assertEquals(2, decodedSegments.size)
        assertEquals("Good morning everyone.", decodedSegments[0].text)
        assertEquals(0L, decodedSegments[0].startMs)
        assertEquals(3200L, decodedSegments[0].endMs)
        assertEquals("Today we will learn database normalization.", decodedSegments[1].text)
    }

    @Test
    fun testSubtitleFormatter_srt() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 1250, endMs = 4500, text = "Welcome to offline AI."),
            TranscriptSegment(id = 2, startMs = 4600, endMs = 9120, text = "Zero cloud dependency.")
        )

        val srt = SubtitleFormatter.toSrt(segments)
        val expected = """
            1
            00:00:01,250 --> 00:00:04,500
            Welcome to offline AI.

            2
            00:00:04,600 --> 00:00:09,120
            Zero cloud dependency.


        """.trimIndent()

        assertEquals(expected.trim(), srt.trim())
    }

    @Test
    fun testSubtitleFormatter_vtt() {
        val segments = listOf(
            TranscriptSegment(id = 1, startMs = 1250, endMs = 4500, text = "Welcome to offline AI.")
        )

        val vtt = SubtitleFormatter.toVtt(segments)
        assertTrue(vtt.startsWith("WEBVTT"))
        assertTrue(vtt.contains("00:00:01.250 --> 00:00:04.500"))
        assertTrue(vtt.contains("Welcome to offline AI."))
    }

    @Test
    fun testSubtitleTimingValidator() {
        // Valid
        assertTrue(SubtitleTimingValidator.validate(1000, 3000, 800, 3500).valid)

        // Negative start
        assertFalse(SubtitleTimingValidator.validate(-10, 3000, null, null).valid)

        // End before start
        assertFalse(SubtitleTimingValidator.validate(3000, 2000, null, null).valid)

        // Too short (< 120ms)
        assertFalse(SubtitleTimingValidator.validate(1000, 1050, null, null).valid)

        // Overlaps previous too much (> 1500ms)
        assertFalse(SubtitleTimingValidator.validate(1000, 5000, 3000, null).valid)
    }
}
