package app.offlinetranscriber.mobile.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream

class Pcm16ChunkReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testWindowReadingAndStepping() {
        val file = tempFolder.newFile("test_audio.pcm")
        // 10 seconds of 16kHz audio = 160,000 samples = 320,000 bytes
        val totalSamples = 160_000
        val bytes = ByteArray(totalSamples * 2)
        FileOutputStream(file).use { it.write(bytes) }

        // Window size: 4 seconds (64,000 samples), overlap: 1 second (16,000 samples)
        // Step: 3 seconds (48,000 samples)
        val reader = Pcm16ChunkReader(windowSeconds = 4, overlapSeconds = 1)
        assertEquals(totalSamples.toLong(), reader.totalSamples(file))

        // Chunk 0: sample 0 to 64,000
        val chunk0 = reader.read(file, startSample = 0L, index = 0)
        assertNotNull(chunk0)
        assertEquals(0, chunk0!!.index)
        assertEquals(0L, chunk0.startSample)
        assertEquals(64_000, chunk0.samples.size)
        assertFalse(chunk0.isLast)
        assertEquals(0L, chunk0.startMs)

        // Next start should be 0 + (64,000 - 16,000) = 48,000
        val nextStart1 = reader.nextStart(chunk0)
        assertEquals(48_000L, nextStart1)

        // Chunk 1: sample 48,000 to 112,000
        val chunk1 = reader.read(file, startSample = nextStart1, index = 1)
        assertNotNull(chunk1)
        assertEquals(1, chunk1!!.index)
        assertEquals(48_000L, chunk1.startSample)
        assertEquals(64_000, chunk1.samples.size)
        assertFalse(chunk1.isLast)
        assertEquals(3_000L, chunk1.startMs)

        // Next start should be 48,000 + 48,000 = 96,000
        val nextStart2 = reader.nextStart(chunk1)
        assertEquals(96_000L, nextStart2)

        // Chunk 2: sample 96,000 to 160,000 (remaining is 64,000, so exactly reaches end)
        val chunk2 = reader.read(file, startSample = nextStart2, index = 2)
        assertNotNull(chunk2)
        assertEquals(2, chunk2!!.index)
        assertEquals(96_000L, chunk2.startSample)
        assertEquals(64_000, chunk2.samples.size)
        assertTrue(chunk2.isLast)

        // Reading past total samples should return null
        val nextStart3 = reader.nextStart(chunk2)
        val chunk3 = reader.read(file, startSample = nextStart3, index = 3)
        assertNull(chunk3)
    }
}
