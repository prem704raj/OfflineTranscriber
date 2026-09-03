package app.offlinetranscriber.mobile.speaker.audio

import android.app.ActivityManager
import android.content.Context

data class DiarizationWindowConfig(
    val windowDurationSeconds: Int,
    val overlapDurationSeconds: Int,
    val sampleRate: Int = 16000
) {
    val windowSamples: Int = windowDurationSeconds * sampleRate
    val overlapSamples: Int = overlapDurationSeconds * sampleRate
    val stepSamples: Int = (windowDurationSeconds - overlapDurationSeconds) * sampleRate
    val stepDurationSeconds: Int = windowDurationSeconds - overlapDurationSeconds
}

object DiarizationWindowPolicy {

    fun resolveConfig(context: Context): DiarizationWindowConfig {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMemGb = memoryInfo.totalMem / (1024L * 1024L * 1024L)

        return when {
            totalMemGb < 4L -> {
                // Low RAM (< 4GB): 4m window, 30s overlap
                DiarizationWindowConfig(
                    windowDurationSeconds = 240,
                    overlapDurationSeconds = 30
                )
            }
            totalMemGb < 8L -> {
                // Normal RAM (4-8GB): 8m window, 30s overlap
                DiarizationWindowConfig(
                    windowDurationSeconds = 480,
                    overlapDurationSeconds = 30
                )
            }
            else -> {
                // High RAM (> 8GB): 12m window, 45s overlap
                DiarizationWindowConfig(
                    windowDurationSeconds = 720,
                    overlapDurationSeconds = 45
                )
            }
        }
    }
}
