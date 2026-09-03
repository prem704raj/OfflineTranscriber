package app.offlinetranscriber.mobile.speaker.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Pcm16WindowReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testReadWindowSamplesNormalized() {
        val file = tempFolder.newFile("test_audio.pcm")
        val sampleCount = 16000 // 1 second at 16kHz
        val byteBuffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until sampleCount) {
            // Write half amplitude sinusoid or known values
            val sample = if (i % 2 == 0) 16384.toShort() else (-16384).toShort()
            byteBuffer.putShort(sample)
        }

        FileOutputStream(file).use { fos ->
            fos.write(byteBuffer.array())
        }

        val reader = Pcm16WindowReader(file, totalSamples = sampleCount.toLong(), sampleRate = 16000)
        assertEquals(1000L, reader.durationMs)

        val window = reader.readWindow(startSample = 0L, numSamples = 100)
        assertEquals(100, window.size)
        assertEquals(0.5f, window[0], 0.01f)
        assertEquals(-0.5f, window[1], 0.01f)

        reader.close()
    }

    @Test
    fun testReadBeyondEofReturnsAvailableSamples() {
        val file = tempFolder.newFile("test_audio_eof.pcm")
        val sampleCount = 500
        val byteBuffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until sampleCount) {
            byteBuffer.putShort(1000.toShort())
        }
        FileOutputStream(file).use { it.write(byteBuffer.array()) }

        val reader = Pcm16WindowReader(file, totalSamples = sampleCount.toLong(), sampleRate = 16000)
        val window = reader.readWindow(startSample = 400L, numSamples = 200)
        assertEquals(100, window.size) // Only 100 available from 400 to 500

        val emptyWindow = reader.readWindow(startSample = 600L, numSamples = 100)
        assertEquals(0, emptyWindow.size)

        reader.close()
    }
}
