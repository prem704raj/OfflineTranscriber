package app.offlinetranscriber.mobile.transcription

import android.content.Context
import android.util.Log
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WhisperEngine(private val context: Context) {

    companion object {
        private const val TAG = "WhisperEngine"

        val DEFAULT_THREAD_COUNT: Int
            get() = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
    }

    private var contextPtr: Long = 0L
    private var currentLoadedModelPath: String? = null
    private val mutex = Mutex()
    private val isCancelledFlag = AtomicBoolean(false)

    val isModelLoaded: Boolean
        get() = contextPtr != 0L

    val activeModelPath: String?
        get() = currentLoadedModelPath

    suspend fun loadModel(modelFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!modelFile.exists()) {
                    return@withContext Result.failure(IllegalArgumentException("Model file does not exist: ${modelFile.absolutePath}"))
                }

                if (currentLoadedModelPath == modelFile.absolutePath && contextPtr != 0L) {
                    Log.i(TAG, "Model already loaded: ${modelFile.name}")
                    return@withContext Result.success(Unit)
                }

                releaseInternal()

                Log.i(TAG, "Initializing Whisper model from: ${modelFile.absolutePath}")
                val ptr = WhisperNative.initContext(modelFile.absolutePath)
                if (ptr == 0L) {
                    val message = if (modelFile.name.contains("large") || modelFile.name.contains("turbo")) {
                        "Accurate model could not run on this device. Try Balanced."
                    } else {
                        "Failed to initialize Whisper model from ${modelFile.name}"
                    }
                    return@withContext Result.failure(RuntimeException(message))
                }

                contextPtr = ptr
                currentLoadedModelPath = modelFile.absolutePath
                Log.i(TAG, "Successfully loaded Whisper model, context pointer: $ptr")
                Result.success(Unit)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM loading model", e)
                Result.failure(RuntimeException("Accurate model could not run on this device. Try Balanced.", e))
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model", e)
                Result.failure(e)
            }
        }
    }

    suspend fun loadModelFromAsset(assetPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                releaseInternal()
                Log.i(TAG, "Initializing Whisper model from asset: $assetPath")
                val ptr = WhisperNative.initContextFromAsset(context.assets, assetPath)
                if (ptr == 0L) {
                    return@withContext Result.failure(RuntimeException("Failed to initialize Whisper model from asset $assetPath"))
                }

                contextPtr = ptr
                currentLoadedModelPath = "asset://$assetPath"
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model from asset", e)
                Result.failure(e)
            }
        }
    }

    fun cancelTranscription() {
        Log.i(TAG, "Cancellation requested for active transcription")
        isCancelledFlag.set(true)
    }

    suspend fun transcribe(
        audioSamples: FloatArray,
        language: String = "auto",
        translate: Boolean = false,
        numThreads: Int = DEFAULT_THREAD_COUNT,
        onProgress: ((Int) -> Unit)? = null,
        onNewSegment: ((TranscriptSegment) -> Unit)? = null
    ): Result<List<TranscriptSegment>> = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (contextPtr == 0L) {
                return@withContext Result.failure(IllegalStateException("No Whisper model is loaded"))
            }

            isCancelledFlag.set(false)
            val segments = mutableListOf<TranscriptSegment>()

            val listener = object : TranscriptionListener {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke(progress)
                }

                override fun onNewSegment(startMs: Long, endMs: Long, text: String) {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        val segment = TranscriptSegment(startMs, endMs, trimmed)
                        segments.add(segment)
                        onNewSegment?.invoke(segment)
                    }
                }

                override fun isCancelled(): Boolean {
                    return isCancelledFlag.get()
                }
            }

            Log.i(TAG, "Running transcription: samples=${audioSamples.size}, lang=$language, translate=$translate, threads=$numThreads")
            val code = app.offlinetranscriber.mobile.processing.HeavyAudioMlArbiter.withLease(
                app.offlinetranscriber.mobile.processing.HeavyMlTaskType.WHISPER_TRANSCRIPTION
            ) {
                WhisperNative.fullTranscribe(
                    contextPtr = contextPtr,
                    numThreads = numThreads,
                    audioData = audioSamples,
                    lang = language,
                    translate = translate,
                    listener = listener
                )
            }

            if (isCancelledFlag.get()) {
                Log.w(TAG, "Transcription was cancelled by user")
                return@withContext Result.failure(InterruptedException("Transcription was cancelled"))
            }

            if (code != 0) {
                Log.e(TAG, "Whisper transcription failed with error code: $code")
                return@withContext Result.failure(RuntimeException("Transcription failed with error code: $code"))
            }

            // Fallback: If no segments were captured via callback, pull directly from context
            if (segments.isEmpty()) {
                val count = WhisperNative.getTextSegmentCount(contextPtr)
                for (i in 0 until count) {
                    val t0 = WhisperNative.getTextSegmentT0(contextPtr, i)
                    val t1 = WhisperNative.getTextSegmentT1(contextPtr, i)
                    val text = WhisperNative.getTextSegment(contextPtr, i).trim()
                    if (text.isNotEmpty()) {
                        segments.add(TranscriptSegment(t0, t1, text))
                    }
                }
            }

            Log.i(TAG, "Transcription complete with ${segments.size} segments")
            Result.success(segments)
        }
    }

    suspend fun releaseIdleContext() = withContext(Dispatchers.IO) {
        mutex.withLock {
            releaseInternal()
        }
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        mutex.withLock {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        if (contextPtr != 0L) {
            Log.i(TAG, "Releasing Whisper context: $contextPtr")
            WhisperNative.freeContext(contextPtr)
            contextPtr = 0L
            currentLoadedModelPath = null
        }
    }
}
