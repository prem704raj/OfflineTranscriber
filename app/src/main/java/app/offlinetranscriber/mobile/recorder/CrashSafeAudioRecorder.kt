package app.offlinetranscriber.mobile.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class CrashSafeAudioRecorder(
    private val sampleRate: Int = WavAudioWriter.SAMPLE_RATE_16K,
    private val audioSource: Int = MediaRecorder.AudioSource.MIC
) {

    companion object {
        private const val TAG = "CrashSafeAudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputStream: BufferedOutputStream? = null
    private var currentFile: File? = null

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    @Volatile
    var hasWriteError: Boolean = false
        private set

    var onErrorListener: ((Exception) -> Unit)? = null

    @Volatile
    private var latestAmplitude: Float = 0f

    @Volatile
    private var totalBytesWritten: Long = 0L

    val activeFile: File?
        get() = currentFile

    val recording: Boolean
        get() = isRecording.get()

    val paused: Boolean
        get() = isPaused.get()

    val maxAmplitude: Float
        get() {
            val amp = latestAmplitude
            latestAmplitude = 0f
            return amp
        }

    val durationMs: Long
        get() = (totalBytesWritten * 1000L) / (sampleRate * WavAudioWriter.CHANNELS_MONO * WavAudioWriter.BYTES_PER_SAMPLE)

    @SuppressLint("MissingPermission")
    fun start(outputFile: File): Result<Unit> {
        if (isRecording.get()) {
            return Result.failure(IllegalStateException("Recorder is already recording"))
        }

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufSize <= 0) {
                return Result.failure(IllegalStateException("AudioRecord hardware buffer initialization failed (code $minBufSize)"))
            }

            val bufferSize = maxOf(minBufSize * 2, 4096)
            val record = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return Result.failure(IllegalStateException("AudioRecord failed to initialize with state ${record.state}"))
            }

            // Write initial 44-byte WAV header placeholder
            WavAudioWriter.writeInitialHeader(outputFile, sampleRate)

            val fos = FileOutputStream(outputFile, true)
            val bos = BufferedOutputStream(fos, bufferSize)

            audioRecord = record
            outputStream = bos
            currentFile = outputFile
            totalBytesWritten = 0L
            latestAmplitude = 0f
            isPaused.set(false)
            isRecording.set(true)

            record.startRecording()

            val thread = Thread({
                recordLoop(record, bos, bufferSize)
            }, "CrashSafeAudioRecorder-Writer")
            recordingThread = thread
            thread.start()

            Log.i(TAG, "Started crash-safe audio recording to ${outputFile.absolutePath}")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            release()
            return Result.failure(e)
        }
    }

    private fun recordLoop(record: AudioRecord, stream: BufferedOutputStream, bufferSize: Int) {
        val shortBuffer = ShortArray(bufferSize / 2)
        val byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

        try {
            while (isRecording.get()) {
                if (isPaused.get()) {
                    try {
                        Thread.sleep(40)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                val readCount = record.read(shortBuffer, 0, shortBuffer.size)
                if (readCount > 0) {
                    var peak = 0
                    byteBuffer.clear()
                    for (i in 0 until readCount) {
                        val s = shortBuffer[i].toInt()
                        val mag = abs(s)
                        if (mag > peak) peak = mag
                        byteBuffer.putShort(shortBuffer[i])
                    }

                    latestAmplitude = peak.toFloat()
                    val bytesToWrite = readCount * 2
                    stream.write(byteBuffer.array(), 0, bytesToWrite)
                    totalBytesWritten += bytesToWrite
                } else if (readCount < 0) {
                    Log.w(TAG, "AudioRecord.read returned error code: $readCount")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in audio recording loop", e)
            hasWriteError = true
            isRecording.set(false)
            onErrorListener?.invoke(e)
        } finally {
            try {
                stream.flush()
            } catch (_: Exception) {}
        }
    }

    fun pause() {
        if (isRecording.get() && !isPaused.get()) {
            isPaused.set(true)
            latestAmplitude = 0f
            flushAndSyncHeader()
            Log.d(TAG, "Recording paused")
        }
    }

    fun resume() {
        if (isRecording.get() && isPaused.get()) {
            isPaused.set(false)
            Log.d(TAG, "Recording resumed")
        }
    }

    fun flushAndSyncHeader() {
        val file = currentFile ?: return
        try {
            outputStream?.flush()
            WavAudioWriter.repairWavHeader(file, sampleRate)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync WAV header: ${e.message}")
        }
    }

    fun stop(): Result<Long> {
        if (!isRecording.get()) {
            return Result.failure(IllegalStateException("Not recording"))
        }

        isRecording.set(false)
        isPaused.set(false)

        try {
            recordingThread?.join(1500)
        } catch (_: Exception) {}
        recordingThread = null

        val record = audioRecord
        audioRecord = null

        runCatching {
            record?.stop()
        }
        runCatching {
            record?.release()
        }

        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

        val file = currentFile
        if (file != null && file.exists()) {
            WavAudioWriter.repairWavHeader(file, sampleRate)
        }

        if (hasWriteError) {
            return Result.failure(java.io.IOException("Audio recording terminated early due to disk write failure"))
        }

        val finalDuration = durationMs
        Log.i(TAG, "Stopped recording. Total duration: ${finalDuration}ms, file: ${file?.absolutePath}")
        return Result.success(finalDuration)
    }

    fun release() {
        isRecording.set(false)
        isPaused.set(false)

        try {
            recordingThread?.interrupt()
            recordingThread?.join(500)
        } catch (_: Exception) {}
        recordingThread = null

        val record = audioRecord
        audioRecord = null
        runCatching { record?.stop() }
        runCatching { record?.release() }

        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

        val file = currentFile
        if (file != null && file.exists()) {
            WavAudioWriter.repairWavHeader(file, sampleRate)
        }
    }
}

