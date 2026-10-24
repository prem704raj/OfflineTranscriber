package app.offlinetranscriber.mobile.recorder

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavAudioWriter {

    const val HEADER_SIZE = 44
    const val SAMPLE_RATE_16K = 16_000
    const val CHANNELS_MONO = 1
    const val BITS_PER_SAMPLE_16 = 16
    const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE_16 / 8

    /**
     * Builds the standard 44-byte canonical PCM WAV header bytes.
     */
    fun createWavHeader(
        totalAudioBytes: Long,
        sampleRate: Int = SAMPLE_RATE_16K,
        channels: Int = CHANNELS_MONO,
        bitsPerSample: Int = BITS_PER_SAMPLE_16
    ): ByteArray {
        val totalDataLen = totalAudioBytes + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteArray(HEADER_SIZE)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt(totalDataLen.toInt())
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // fmt chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1.toShort()) // AudioFormat 1 = PCM
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // data chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt(totalAudioBytes.toInt())

        return header
    }

    /**
     * Writes the initial WAV header into a new or truncated file.
     */
    fun writeInitialHeader(
        file: File,
        sampleRate: Int = SAMPLE_RATE_16K,
        channels: Int = CHANNELS_MONO,
        bitsPerSample: Int = BITS_PER_SAMPLE_16
    ) {
        val header = createWavHeader(0L, sampleRate, channels, bitsPerSample)
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(header)
        }
    }

    /**
     * Updates the RIFF chunk size and data chunk size in place based on the physical file length.
     * This provides crash safety: if an unclosed recording exists, calling this function
     * immediately seals it into a 100% compliant, playable WAV file.
     */
    fun repairWavHeader(
        file: File,
        sampleRate: Int = SAMPLE_RATE_16K,
        channels: Int = CHANNELS_MONO,
        bitsPerSample: Int = BITS_PER_SAMPLE_16
    ): Boolean {
        if (!file.exists()) return false
        val fileLength = file.length()
        if (fileLength < HEADER_SIZE) return false

        // Safety guard: Never touch non-WAV files (e.g. .m4a, .mp4, .aac)
        if (!file.name.endsWith(".wav", ignoreCase = true)) {
            return false
        }

        val audioBytes = fileLength - HEADER_SIZE
        val riffChunkSize = fileLength - 8

        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

        return try {
            RandomAccessFile(file, "rw").use { raf ->
                // Check for MP4/M4A ftyp atom or other container signatures
                val magic = ByteArray(8)
                raf.readFully(magic)
                val isM4a = magic[4] == 'f'.code.toByte() &&
                    magic[5] == 't'.code.toByte() &&
                    magic[6] == 'y'.code.toByte() &&
                    magic[7] == 'p'.code.toByte()
                if (isM4a) {
                    // Refuse to touch an MP4/M4A container file
                    return false
                }

                val isRiff = magic[0] == 'R'.code.toByte() &&
                    magic[1] == 'I'.code.toByte() &&
                    magic[2] == 'F'.code.toByte() &&
                    magic[3] == 'F'.code.toByte()

                if (!isRiff) {
                    // Only rewrite header if file is a dedicated .wav raw PCM without existing RIFF
                    val header = createWavHeader(audioBytes, sampleRate, channels, bitsPerSample)
                    raf.seek(0)
                    raf.write(header)
                } else {
                    // Update RIFF size at offset 4
                    buf.clear()
                    buf.putInt(riffChunkSize.toInt())
                    raf.seek(4)
                    raf.write(buf.array())

                    // Update data chunk size at offset 40
                    buf.clear()
                    buf.putInt(audioBytes.toInt())
                    raf.seek(40)
                    raf.write(buf.array())
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if the file exists, has a valid WAV header, and contains non-empty audio data.
     */
    fun isValidWav(file: File): Boolean {
        if (!file.exists() || file.length() <= HEADER_SIZE) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                magic[0] == 'R'.code.toByte() &&
                    magic[1] == 'I'.code.toByte() &&
                    magic[2] == 'F'.code.toByte() &&
                    magic[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Calculates duration in milliseconds from the physical PCM payload.
     */
    fun getDurationMs(
        file: File,
        sampleRate: Int = SAMPLE_RATE_16K,
        channels: Int = CHANNELS_MONO,
        bitsPerSample: Int = BITS_PER_SAMPLE_16
    ): Long {
        if (!file.exists() || file.length() <= HEADER_SIZE) return 0L
        val audioBytes = file.length() - HEADER_SIZE
        val bytesPerSec = sampleRate * channels * (bitsPerSample / 8)
        return if (bytesPerSec > 0) (audioBytes * 1000L) / bytesPerSec else 0L
    }
}

