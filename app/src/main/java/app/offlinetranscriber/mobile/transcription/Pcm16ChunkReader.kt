package app.offlinetranscriber.mobile.transcription

import java.io.File
import java.io.RandomAccessFile

data class PcmWindow(
    val index: Int,
    val startSample: Long,
    val samples: FloatArray,
    val isLast: Boolean
) {
    val startMs: Long
        get() = startSample * 1000L / AudioProcessor.TARGET_SAMPLE_RATE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PcmWindow

        if (index != other.index) return false
        if (startSample != other.startSample) return false
        if (!samples.contentEquals(other.samples)) return false
        if (isLast != other.isLast) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + startSample.hashCode()
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + isLast.hashCode()
        return result
    }
}

class Pcm16ChunkReader(
    private val windowSeconds: Int = 5 * 60,
    private val overlapSeconds: Int = 2
) {
    private val sampleRate = AudioProcessor.TARGET_SAMPLE_RATE
    private val windowSamples = windowSeconds.toLong() * sampleRate
    private val overlapSamples = overlapSeconds.toLong() * sampleRate
    private val stepSamples = windowSamples - overlapSamples

    init {
        require(windowSamples > overlapSamples) { "Window size must be strictly greater than overlap." }
        require(overlapSeconds >= 0) { "Overlap must be non-negative." }
    }

    fun totalSamples(file: File): Long = file.length() / 2L

    fun read(file: File, startSample: Long, index: Int): PcmWindow? {
        val total = totalSamples(file)
        if (startSample >= total) return null

        val count = minOf(windowSamples, total - startSample).toInt()
        val bytes = ByteArray(count * 2)

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startSample * 2L)
            raf.readFully(bytes)
        }

        val samples = FloatArray(count)
        var p = 0
        for (i in 0 until count) {
            val lo = bytes[p++].toInt() and 0xFF
            val hi = bytes[p++].toInt()
            val value = ((hi shl 8) or lo).toShort()
            samples[i] = value / 32768f
        }

        return PcmWindow(
            index = index,
            startSample = startSample,
            samples = samples,
            isLast = startSample + count >= total
        )
    }

    fun nextStart(current: PcmWindow): Long =
        if (current.isLast) {
            current.startSample + current.samples.size
        } else {
            current.startSample + stepSamples
        }
}
