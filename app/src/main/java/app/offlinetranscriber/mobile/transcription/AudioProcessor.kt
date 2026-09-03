package app.offlinetranscriber.mobile.transcription

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.roundToInt

class AudioProcessor(private val context: Context) {

    companion object {
        const val TARGET_SAMPLE_RATE = 16_000
        private const val TIMEOUT_US = 10_000L
    }

    data class AudioMetadata(
        val durationMs: Long,
        val sampleRate: Int,
        val channelCount: Int,
        val mimeType: String
    )

    suspend fun getAudioMetadata(uri: Uri): AudioMetadata? =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                val index = selectAudioTrack(extractor)
                if (index < 0) return@withContext null
                val format = extractor.getTrackFormat(index)
                AudioMetadata(
                    durationMs = format.longOr(MediaFormat.KEY_DURATION, 0L) / 1000L,
                    sampleRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, 44_100),
                    channelCount = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1),
                    mimeType = format.getString(MediaFormat.KEY_MIME) ?: "audio/unknown"
                )
            } catch (_: Throwable) {
                null
            } finally {
                extractor.release()
            }
        }

    suspend fun prepareToPcm16(
        uri: Uri,
        outputFile: File,
        onProgress: suspend (Int) -> Unit = {},
        isCancelled: () -> Boolean = { false }
    ): PreparedPcmAudio = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        outputFile.delete()

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            val audioTrack = selectAudioTrack(extractor)
            require(audioTrack >= 0) { "No supported audio track was found." }

            extractor.selectTrack(audioTrack)
            val inputFormat = extractor.getTrackFormat(audioTrack)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: error("Audio MIME type is missing.")
            val durationUs = inputFormat.longOr(MediaFormat.KEY_DURATION, 0L)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            var inputEnded = false
            var outputEnded = false
            var outputRate = inputFormat.intOr(MediaFormat.KEY_SAMPLE_RATE, 44_100)
            var channels = inputFormat.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
            var pcmEncoding = inputFormat.intOr(
                MediaFormat.KEY_PCM_ENCODING,
                AudioFormat.ENCODING_PCM_16BIT
            )

            var resampler = StreamingLinearResampler(outputRate, TARGET_SAMPLE_RATE)
            var samplesWritten = 0L
            var lastProgress = -1

            BufferedOutputStream(FileOutputStream(outputFile), 128 * 1024).use { output ->
                val info = MediaCodec.BufferInfo()

                while (!outputEnded) {
                    if (isCancelled()) {
                        throw CancellationException("Audio preparation cancelled.")
                    }

                    if (!inputEnded) {
                        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                                ?: error("Decoder input buffer unavailable.")
                            inputBuffer.clear()
                            val size = extractor.readSampleData(inputBuffer, 0)

                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputEnded = true
                            } else {
                                val pts = extractor.sampleTime.coerceAtLeast(0L)
                                codec.queueInputBuffer(inputIndex, 0, size, pts, 0)
                                extractor.advance()

                                if (durationUs > 0L) {
                                    val p = ((pts * 100L) / durationUs)
                                        .toInt().coerceIn(0, 99)
                                    if (p != lastProgress) {
                                        lastProgress = p
                                        onProgress(p)
                                    }
                                }
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val format = codec.outputFormat
                            val newRate = format.intOr(MediaFormat.KEY_SAMPLE_RATE, outputRate)
                            val newChannels = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                            val newEncoding = format.intOr(
                                MediaFormat.KEY_PCM_ENCODING,
                                AudioFormat.ENCODING_PCM_16BIT
                            )

                            if (samplesWritten > 0L && newRate != outputRate) {
                                error("Decoder changed sample rate during the stream.")
                            }

                            outputRate = newRate
                            channels = newChannels
                            pcmEncoding = newEncoding
                            resampler = StreamingLinearResampler(outputRate, TARGET_SAMPLE_RATE)
                        }

                        else -> if (outputIndex >= 0) {
                            val buffer = codec.getOutputBuffer(outputIndex)
                            if (buffer != null && info.size > 0) {
                                val mono = decodeMono(
                                    source = buffer,
                                    offset = info.offset,
                                    size = info.size,
                                    channels = channels,
                                    encoding = pcmEncoding
                                )

                                val at16k = if (outputRate == TARGET_SAMPLE_RATE) {
                                    mono
                                } else {
                                    resampler.process(mono)
                                }

                                writePcm16(output, at16k)
                                samplesWritten += at16k.size
                            }

                            codec.releaseOutputBuffer(outputIndex, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                outputEnded = true
                            }
                        }
                    }
                }

                output.flush()
            }

            require(samplesWritten > 0L) { "Decoded audio contained no PCM samples." }
            onProgress(100)

            PreparedPcmAudio(
                file = outputFile,
                durationMs = (samplesWritten * 1000L / TARGET_SAMPLE_RATE).coerceAtLeast(1L),
                totalSamples = samplesWritten
            )
        } catch (t: Throwable) {
            outputFile.delete()
            throw t
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun decodeMono(
        source: ByteBuffer,
        offset: Int,
        size: Int,
        channels: Int,
        encoding: Int
    ): FloatArray {
        val ch = channels.coerceAtLeast(1)
        val buffer = source.duplicate().apply {
            order(ByteOrder.nativeOrder())
            position(offset)
            limit(offset + size)
        }.slice().order(ByteOrder.nativeOrder())

        return when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floats = buffer.asFloatBuffer()
                val frames = floats.remaining() / ch
                FloatArray(frames) { frame ->
                    var sum = 0f
                    repeat(ch) { c -> sum += floats.get(frame * ch + c) }
                    (sum / ch).coerceIn(-1f, 1f)
                }
            }

            AudioFormat.ENCODING_PCM_16BIT -> {
                val shorts = buffer.asShortBuffer()
                val frames = shorts.remaining() / ch
                FloatArray(frames) { frame ->
                    var sum = 0f
                    repeat(ch) { c -> sum += shorts.get(frame * ch + c) / 32768f }
                    (sum / ch).coerceIn(-1f, 1f)
                }
            }

            else -> error(
                "Unsupported decoder PCM encoding: $encoding. " +
                    "Only PCM16 and PCM float are accepted."
            )
        }
    }

    private fun writePcm16(output: BufferedOutputStream, samples: FloatArray) {
        if (samples.isEmpty()) return
        val bytes = ByteArray(samples.size * 2)
        var p = 0

        for (sample in samples) {
            val value = (sample.coerceIn(-1f, 1f) * 32767f)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bytes[p++] = (value and 0xFF).toByte()
            bytes[p++] = ((value ushr 8) and 0xFF).toByte()
        }
        output.write(bytes)
    }

    private class StreamingLinearResampler(
        private val inputRate: Int,
        private val outputRate: Int
    ) {
        private val step = inputRate.toDouble() / outputRate.toDouble()
        private var processedInput = 0L
        private var nextPosition = 0.0
        private var previousSample: Float? = null

        fun process(input: FloatArray): FloatArray {
            if (input.isEmpty()) return FloatArray(0)
            if (inputRate == outputRate) {
                processedInput += input.size
                previousSample = input.last()
                return input
            }

            val start = processedInput
            val endExclusive = start + input.size
            var out = FloatArray(((input.size / step) + 4).toInt().coerceAtLeast(4))
            var count = 0

            while (true) {
                val i0 = floor(nextPosition).toLong()
                val i1 = i0 + 1L
                if (i1 >= endExclusive) break

                if (i0 < start - 1L) {
                    nextPosition = start.toDouble()
                    continue
                }

                val s0 = when {
                    i0 == start - 1L -> previousSample ?: input.first()
                    i0 >= start -> input[(i0 - start).toInt()]
                    else -> input.first()
                }

                val s1 = input[(i1 - start).toInt()]
                val fraction = (nextPosition - i0.toDouble()).toFloat()
                val value = s0 + (s1 - s0) * fraction

                if (count == out.size) out = out.copyOf(out.size * 2)
                out[count++] = value.coerceIn(-1f, 1f)
                nextPosition += step
            }

            processedInput = endExclusive
            previousSample = input.last()
            return out.copyOf(count)
        }
    }

    suspend fun decodeAudioTo16kHzMono(
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File.createTempFile("legacy_decode_", ".pcm", context.cacheDir)
            try {
                prepareToPcm16(
                    uri = uri,
                    outputFile = tempFile,
                    onProgress = { p -> onProgress(p / 100f) }
                )
                val totalSamples = (tempFile.length() / 2L).toInt()
                val bytes = tempFile.readBytes()
                val floats = FloatArray(totalSamples)
                var p = 0
                for (i in 0 until totalSamples) {
                    val lo = bytes[p++].toInt() and 0xFF
                    val hi = bytes[p++].toInt()
                    val value = ((hi shl 8) or lo).toShort()
                    floats[i] = value / 32768f
                }
                floats
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun decodeFromInputStream(
        inputStream: java.io.InputStream
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            val tempIn = File.createTempFile("legacy_in_", ".tmp", context.cacheDir)
            val tempOut = File.createTempFile("legacy_out_", ".pcm", context.cacheDir)
            try {
                tempIn.outputStream().use { out -> inputStream.copyTo(out) }
                prepareToPcm16(
                    uri = Uri.fromFile(tempIn),
                    outputFile = tempOut
                )
                val totalSamples = (tempOut.length() / 2L).toInt()
                val bytes = tempOut.readBytes()
                val floats = FloatArray(totalSamples)
                var p = 0
                for (i in 0 until totalSamples) {
                    val lo = bytes[p++].toInt() and 0xFF
                    val hi = bytes[p++].toInt()
                    val value = ((hi shl 8) or lo).toShort()
                    floats[i] = value / 32768f
                }
                floats
            } finally {
                tempIn.delete()
                tempOut.delete()
            }
        }
    }

    private fun MediaFormat.intOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private fun MediaFormat.longOr(key: String, fallback: Long): Long =
        if (containsKey(key)) getLong(key) else fallback
}
