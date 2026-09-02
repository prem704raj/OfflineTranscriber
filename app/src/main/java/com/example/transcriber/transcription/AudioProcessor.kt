package com.example.transcriber.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioProcessor"
        const val TARGET_SAMPLE_RATE = 16000
        private const val TIMEOUT_US = 5000L
    }

    data class AudioMetadata(
        val durationMs: Long,
        val sampleRate: Int,
        val channelCount: Int,
        val mimeType: String
    )

    suspend fun getAudioMetadata(uri: Uri): AudioMetadata? = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) return@withContext null

            val format = extractor.getTrackFormat(trackIndex)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/unknown"

            AudioMetadata(
                durationMs = durationUs / 1000,
                sampleRate = sampleRate,
                channelCount = channelCount,
                mimeType = mime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get audio metadata", e)
            null
        } finally {
            extractor.release()
        }
    }

    /**
     * Decodes any audio/video URI (MP3, M4A, WAV, AAC, etc.) into 16kHz mono FloatArray samples.
     */
    suspend fun decodeAudioTo16kHzMono(
        uri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            val audioTrackIndex = selectAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                extractor.release()
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

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val pcmDataStream = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()

            var isExtractorEOS = false
            var isDecoderEOS = false

            var outputSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            var outputChannelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

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
                                    onProgress?.invoke(progress)
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

                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmDataStream.write(chunk)
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
                    Log.i(TAG, "Decoder output format changed: sampleRate=$outputSampleRate, channels=$outputChannelCount")
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            val rawBytes = pcmDataStream.toByteArray()
            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Decoded PCM data is empty"))
            }

            // Convert 16-bit PCM bytes to mono float array
            val monoSamples = convertPcmBytesToMonoFloat(rawBytes, outputChannelCount)

            // Resample to 16kHz if needed
            val finalSamples = if (outputSampleRate != TARGET_SAMPLE_RATE) {
                resampleLinear(monoSamples, outputSampleRate, TARGET_SAMPLE_RATE)
            } else {
                monoSamples
            }

            Log.i(TAG, "Audio successfully decoded to 16kHz mono: ${finalSamples.size} samples (${finalSamples.size / TARGET_SAMPLE_RATE}s)")
            onProgress?.invoke(1f)
            Result.success(finalSamples)

        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio", e)
            Result.failure(e)
        }
    }

    /**
     * Decodes a bundled raw resource or asset WAV/MP3 directly.
     */
    suspend fun decodeFromInputStream(
        inputStream: InputStream,
        totalEstimatedBytes: Long = 0,
        onProgress: ((Float) -> Unit)? = null
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        try {
            // Write to a temporary file for MediaExtractor
            val tempFile = File.createTempFile("temp_audio_", ".tmp", context.cacheDir)
            tempFile.deleteOnExit()
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val result = decodeAudioTo16kHzMono(Uri.fromFile(tempFile), onProgress)
            tempFile.delete()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode from stream", e)
            Result.failure(e)
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }

    /**
     * Converts raw 16-bit Little Endian PCM byte buffer into normalized mono floats [-1.0f, 1.0f].
     */
    private fun convertPcmBytesToMonoFloat(bytes: ByteArray, channelCount: Int): FloatArray {
        val shortBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val numShorts = shortBuffer.remaining()
        val validChannels = channelCount.coerceAtLeast(1)
        val numFrames = numShorts / validChannels

        val monoFloats = FloatArray(numFrames)

        for (i in 0 until numFrames) {
            var sum = 0.0f
            for (c in 0 until validChannels) {
                val sampleShort = shortBuffer.get(i * validChannels + c)
                sum += sampleShort / 32768.0f
            }
            monoFloats[i] = (sum / validChannels).coerceIn(-1.0f, 1.0f)
        }

        return monoFloats
    }

    /**
     * High quality linear resampler from inRate to outRate.
     */
    private fun resampleLinear(input: FloatArray, inRate: Int, outRate: Int): FloatArray {
        if (input.isEmpty() || inRate == outRate) return input

        val ratio = inRate.toDouble() / outRate.toDouble()
        val outputLength = (input.size / ratio).toInt()
        val output = FloatArray(outputLength)

        for (i in 0 until outputLength) {
            val sourcePos = i * ratio
            val index1 = sourcePos.toInt()
            val index2 = (index1 + 1).coerceAtMost(input.size - 1)
            val frac = (sourcePos - index1).toFloat()

            val sample1 = input[index1]
            val sample2 = input[index2]
            output[i] = sample1 + frac * (sample2 - sample1)
        }

        return output
    }
}
