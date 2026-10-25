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

    const val MAX_WAV_AUDIO_BYTES = 0x7FFF_0000L // ~2.14 GB upper bound for standard signed 32-bit RIFF

    /**
     * Builds the standard 44-byte canonical PCM WAV header bytes.
     */
    fun createWavHeader(
        totalAudioBytes: Long,
        sampleRate: Int = SAMPLE_RATE_16K,
        channels: Int = CHANNELS_MONO,
        bitsPerSample: Int = BITS_PER_SAMPLE_16
    ): ByteArray {
        val safeAudioBytes = totalAudioBytes.coerceIn(0L, MAX_WAV_AUDIO_BYTES)
        val totalDataLen = safeAudioBytes + 36L
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
        buffer.putInt(safeAudioBytes.toInt())

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
        if (audioBytes > MAX_WAV_AUDIO_BYTES) {
            // The RIFF/data chunk sizes cannot be represented safely. Do not write
            // a header that would claim a smaller payload than the file contains.
            return false
        }
        val riffChunkSize = fileLength - 8L

        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

        return try {
            RandomAccessFile(file, "rw").use { raf ->
                val header = ByteArray(HEADER_SIZE)
                raf.readFully(header)
                val existing = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                // Never overwrite arbitrary data with a synthetic header. Only
                // repair files that already have the canonical PCM layout written
                // by this app, otherwise a truncated/invalid file could be mutated.
                val isRiff = header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'A'.code.toByte() &&
                    header[10] == 'V'.code.toByte() &&
                    header[11] == 'E'.code.toByte() &&
                    header[12] == 'f'.code.toByte() &&
                    header[13] == 'm'.code.toByte() &&
                    header[14] == 't'.code.toByte() &&
                    header[15] == ' '.code.toByte() &&
                    header[36] == 'd'.code.toByte() &&
                    header[37] == 'a'.code.toByte() &&
                    header[38] == 't'.code.toByte() &&
                    header[39] == 'a'.code.toByte()
                if (!isRiff) return false
                if (existing.getInt(16) != 16) return false
                if (existing.getShort(20) != 1.toShort()) return false
                val bytesPerSample = bitsPerSample / 8
                val expectedBlockAlign = channels * bytesPerSample
                if (existing.getShort(22).toInt() != channels) return false
                if (existing.getInt(24) != sampleRate) return false
                if (existing.getInt(28).toLong() and 0xFFFF_FFFFL !=
                    (sampleRate.toLong() * expectedBlockAlign)
                ) return false
                if (existing.getShort(32).toInt() != expectedBlockAlign) return false
                if (existing.getShort(34).toInt() != bitsPerSample) return false

                buf.clear()
                buf.putInt(riffChunkSize.toInt())
                raf.seek(4)
                raf.write(buf.array())

                buf.clear()
                buf.putInt(audioBytes.toInt())
                raf.seek(40)
                raf.write(buf.array())
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
                val header = ByteArray(HEADER_SIZE)
                raf.readFully(header)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                // 1. RIFF chunk identifier
                if (buffer.get(0) != 'R'.code.toByte() ||
                    buffer.get(1) != 'I'.code.toByte() ||
                    buffer.get(2) != 'F'.code.toByte() ||
                    buffer.get(3) != 'F'.code.toByte()
                ) return false

                // 2. WAVE format identifier
                if (buffer.get(8) != 'W'.code.toByte() ||
                    buffer.get(9) != 'A'.code.toByte() ||
                    buffer.get(10) != 'V'.code.toByte() ||
                    buffer.get(11) != 'E'.code.toByte()
                ) return false

                // 3. fmt subchunk identifier
                if (buffer.get(12) != 'f'.code.toByte() ||
                    buffer.get(13) != 'm'.code.toByte() ||
                    buffer.get(14) != 't'.code.toByte() ||
                    buffer.get(15) != ' '.code.toByte()
                ) return false

                val fmtSize = buffer.getInt(16)
                if (fmtSize != 16) return false

                val audioFormat = buffer.getShort(20)
                if (audioFormat != 1.toShort()) return false // Must be PCM format

                val numChannels = buffer.getShort(22).toInt()
                if (numChannels <= 0 || numChannels > 8) return false

                val sampleRate = buffer.getInt(24)
                if (sampleRate < 8_000 || sampleRate > 192_000) return false

                val bitsPerSample = buffer.getShort(34).toInt()
                if (bitsPerSample != 8 && bitsPerSample != 16 && bitsPerSample != 24 && bitsPerSample != 32) return false

                val bytesPerSample = bitsPerSample / 8
                val blockAlign = buffer.getShort(32).toInt()
                if (blockAlign != numChannels * bytesPerSample) return false

                val expectedByteRate = sampleRate.toLong() * blockAlign
                if (buffer.getInt(28).toLong() and 0xFFFF_FFFFL != expectedByteRate) return false

                // 4. data subchunk identifier
                if (buffer.get(36) != 'd'.code.toByte() ||
                    buffer.get(37) != 'a'.code.toByte() ||
                    buffer.get(38) != 't'.code.toByte() ||
                    buffer.get(39) != 'a'.code.toByte()
                ) return false

                val riffSize = buffer.getInt(4).toLong() and 0xFFFF_FFFFL
                val dataSize = buffer.getInt(40).toLong() and 0xFFFF_FFFFL
                val availablePayload = file.length() - HEADER_SIZE
                if (dataSize <= 0L || dataSize > availablePayload) return false
                if (dataSize % blockAlign != 0L) return false
                if (riffSize < 36L + dataSize || riffSize > file.length() - 8L) return false

                true
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
        val audioBytes = (file.length() - HEADER_SIZE).coerceIn(0L, MAX_WAV_AUDIO_BYTES)
        val bytesPerSec = sampleRate * channels * (bitsPerSample / 8)
        return if (bytesPerSec > 0) (audioBytes * 1000L) / bytesPerSec else 0L
    }
}

