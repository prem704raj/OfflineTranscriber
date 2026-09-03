package app.offlinetranscriber.mobile.speaker.audio

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Pcm16WindowReader(
    private val pcmFile: File,
    val totalSamples: Long,
    val sampleRate: Int = 16000
) : Closeable {

    private val raf = RandomAccessFile(pcmFile, "r")

    val durationMs: Long = (totalSamples * 1000L) / sampleRate

    fun readWindow(startSample: Long, numSamples: Int): FloatArray {
        if (startSample >= totalSamples || numSamples <= 0) {
            return FloatArray(0)
        }

        val availableSamples = (totalSamples - startSample).toInt()
        val actualSamplesToRead = minOf(numSamples, availableSamples)
        val bytesToRead = actualSamplesToRead * 2

        val byteBuffer = ByteBuffer.allocate(bytesToRead).order(ByteOrder.LITTLE_ENDIAN)
        val fileOffset = startSample * 2L

        synchronized(raf) {
            raf.seek(fileOffset)
            raf.readFully(byteBuffer.array(), 0, bytesToRead)
        }

        val result = FloatArray(actualSamplesToRead)
        val shortBuffer = byteBuffer.asShortBuffer()
        for (i in 0 until actualSamplesToRead) {
            val sampleShort = shortBuffer.get(i)
            result[i] = sampleShort / 32768.0f
        }

        return result
    }

    fun readTimeRange(startMs: Long, endMs: Long): FloatArray {
        val startSample = ((maxOf(0L, startMs) * sampleRate) / 1000L).coerceAtMost(totalSamples)
        val endSample = ((maxOf(startMs, endMs) * sampleRate) / 1000L).coerceAtMost(totalSamples)
        val sampleCount = (endSample - startSample).toInt()
        return readWindow(startSample, sampleCount)
    }

    override fun close() {
        try {
            raf.close()
        } catch (_: Exception) {}
    }
}
