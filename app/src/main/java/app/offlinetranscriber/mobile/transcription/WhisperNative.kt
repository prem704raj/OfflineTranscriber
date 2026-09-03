package app.offlinetranscriber.mobile.transcription

import android.content.res.AssetManager
import android.util.Log

interface TranscriptionListener {
    fun onProgress(progress: Int)
    fun onNewSegment(startMs: Long, endMs: Long, text: String)
    fun isCancelled(): Boolean
}

object WhisperNative {
    private const val TAG = "WhisperNative"

    init {
        try {
            System.loadLibrary("whisper_jni")
            Log.i(TAG, "Successfully loaded libwhisper_jni.so")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libwhisper_jni.so", e)
        }
    }

    external fun initContext(modelPath: String): Long
    external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long
    external fun freeContext(contextPtr: Long)
    external fun fullTranscribe(
        contextPtr: Long,
        numThreads: Int,
        audioData: FloatArray,
        lang: String,
        translate: Boolean,
        listener: TranscriptionListener?
    ): Int
    external fun getTextSegmentCount(contextPtr: Long): Int
    external fun getTextSegment(contextPtr: Long, index: Int): String
    external fun getTextSegmentT0(contextPtr: Long, index: Int): Long
    external fun getTextSegmentT1(contextPtr: Long, index: Int): Long
    external fun getSystemInfo(): String
}
