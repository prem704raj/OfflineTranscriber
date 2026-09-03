package app.offlinetranscriber.mobile.backup

import app.offlinetranscriber.mobile.backup.fingerprint.FingerprintSegment
import app.offlinetranscriber.mobile.backup.fingerprint.TranscriptFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TranscriptFingerprintTest {

    @Test
    fun testExactMatchProducesIdenticalFingerprint() {
        val segs1 = listOf(
            FingerprintSegment(0L, 2000L, "Hello world"),
            FingerprintSegment(2000L, 4500L, "This is a transcript.")
        )
        val segs2 = listOf(
            FingerprintSegment(0L, 2000L, "Hello world"),
            FingerprintSegment(2000L, 4500L, "This is a transcript.")
        )

        val fp1 = TranscriptFingerprint.calculate(4500L, "AUDIO", segs1.asSequence())
        val fp2 = TranscriptFingerprint.calculate(4500L, "AUDIO", segs2.asSequence())

        assertEquals(fp1, fp2)
    }

    @Test
    fun testWhitespaceNormalizationProducesIdenticalFingerprint() {
        val segs1 = listOf(
            FingerprintSegment(0L, 2000L, "  Hello    world  \n")
        )
        val segs2 = listOf(
            FingerprintSegment(0L, 2000L, "Hello world")
        )

        val fp1 = TranscriptFingerprint.calculate(2000L, "AUDIO", segs1.asSequence())
        val fp2 = TranscriptFingerprint.calculate(2000L, "AUDIO", segs2.asSequence())

        assertEquals(fp1, fp2)
    }

    @Test
    fun testDifferentTimingProducesDifferentFingerprint() {
        val segs1 = listOf(
            FingerprintSegment(0L, 2000L, "Hello world")
        )
        val segs2 = listOf(
            FingerprintSegment(500L, 2000L, "Hello world")
        )

        val fp1 = TranscriptFingerprint.calculate(2000L, "AUDIO", segs1.asSequence())
        val fp2 = TranscriptFingerprint.calculate(2000L, "AUDIO", segs2.asSequence())

        assertNotEquals(fp1, fp2)
    }

    @Test
    fun testDifferentMediaTypeProducesDifferentFingerprint() {
        val segs = listOf(
            FingerprintSegment(0L, 2000L, "Hello world")
        )

        val fpAudio = TranscriptFingerprint.calculate(2000L, "AUDIO", segs.asSequence())
        val fpVideo = TranscriptFingerprint.calculate(2000L, "VIDEO", segs.asSequence())

        assertNotEquals(fpAudio, fpVideo)
    }
}
