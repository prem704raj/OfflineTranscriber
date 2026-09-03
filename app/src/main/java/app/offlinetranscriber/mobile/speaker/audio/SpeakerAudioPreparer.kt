package app.offlinetranscriber.mobile.speaker.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

data class PreparedAudioResult(
    val pcmFile: File,
    val totalSamples: Long,
    val durationMs: Long,
    val sampleRate: Int = 16000
)

class SpeakerAudioPreparer(
    private val context: Context
) {
    companion object {
        private const val TIMEOUT_US = 5000L
        private const val TARGET_SAMPLE_RATE = 16000
    }

    suspend fun preparePcm16(
        uri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<PreparedAudioResult> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "speaker_temp").apply { mkdirs() }
        val tempFile = File(tempDir, "audio_16k_${UUID.randomUUID()}.pcm")

        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var outputStream: BufferedOutputStream? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            val audioTrackIndex = selectAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return@withContext Result.failure(IllegalArgumentException("No audio track found in media file"))
            }

            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return@withContext Result.failure(IllegalArgumentException("Audio MIME type not found"))

            val totalDurationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                1L
            }

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            outputStream = BufferedOutputStream(FileOutputStream(tempFile), 65536)
            val bufferInfo = MediaCodec.BufferInfo()

            var isExtractorEOS = false
            var isDecoderEOS = false

            var outputSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            var outputChannelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            var totalPcm16SamplesWritten = 0L

            // Resampling state
            var resampleRatio = outputSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
            var resampleAccumulator = 0.0

            while (!isDecoderEOS) {
                // 1. Feed input buffer
                if (!isExtractorEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inputBufferIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isExtractorEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                decoder.queueInputBuffer(
                                    inputBufferIndex, 0, sampleSize, presentationTimeUs, 0
                                )
                                extractor.advance()

                                if (totalDurationUs > 0) {
                                    val progress = (presentationTimeUs.toFloat() / totalDurationUs).coerceIn(0f, 1f)
                                    onProgress?.invoke(progress * 0.9f)
                                }
                            }
                        }
                    }
                }

                // 2. Read output buffer
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                        val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val shortArray = ShortArray(shortBuffer.remaining())
                        shortBuffer.get(shortArray)

                        // Convert to mono
                        val monoShorts = if (outputChannelCount > 1) {
                            val frames = shortArray.size / outputChannelCount
                            val mono = ShortArray(frames)
                            for (i in 0 until frames) {
                                var sum = 0
                                for (c in 0 until outputChannelCount) {
                                    sum += shortArray[i * outputChannelCount + c]
                                }
                                mono[i] = (sum / outputChannelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }
                            mono
                        } else {
                            shortArray
                        }

                        // Resample to 16kHz if needed
                        if (outputSampleRate == TARGET_SAMPLE_RATE) {
                            val byteBuffer = ByteBuffer.allocate(monoShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (s in monoShorts) {
                                byteBuffer.putShort(s)
                            }
                            outputStream.write(byteBuffer.array())
                            totalPcm16SamplesWritten += monoShorts.size
                        } else {
                            val byteBuffer = ByteBuffer.allocate(monoShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                            var inIdx = 0
                            while (resampleAccumulator < monoShorts.size) {
                                val srcIndex = resampleAccumulator.toInt()
                                if (srcIndex < monoShorts.size) {
                                    byteBuffer.putShort(monoShorts[srcIndex])
                                    totalPcm16SamplesWritten++
                                }
                                resampleAccumulator += resampleRatio
                            }
                            resampleAccumulator -= monoShorts.size
                            if (byteBuffer.position() > 0) {
                                outputStream.write(byteBuffer.array(), 0, byteBuffer.position())
                            }
                        }
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isDecoderEOS = true
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        outputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        outputChannelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    resampleRatio = outputSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
                }
            }

            outputStream.flush()
            onProgress?.invoke(1.0f)

            val durationMs = (totalPcm16SamplesWritten * 1000L) / TARGET_SAMPLE_RATE

            Result.success(
                PreparedAudioResult(
                    pcmFile = tempFile,
                    totalSamples = totalPcm16SamplesWritten,
                    durationMs = durationMs,
                    sampleRate = TARGET_SAMPLE_RATE
                )
            )
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
}
