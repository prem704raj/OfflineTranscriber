package com.example.transcriber.modelmanager

import android.app.ActivityManager
import android.content.Context

data class DeviceProfile(
    val totalRamBytes: Long,
    val cpuCores: Int
)

class DeviceModelAdvisor(
    private val context: Context
) {
    fun profile(): DeviceProfile {
        val manager =
            context.getSystemService(
                ActivityManager::class.java
            )

        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)

        return DeviceProfile(
            totalRamBytes = info.totalMem,
            cpuCores =
                Runtime.getRuntime()
                    .availableProcessors()
                    .coerceAtLeast(1)
        )
    }

    fun recommend(): WhisperModelSpec {
        return recommendForProfile(profile())
    }

    companion object {
        fun recommendForProfile(profile: DeviceProfile): WhisperModelSpec {
            val gb =
                profile.totalRamBytes.toDouble() /
                    (1024.0 * 1024.0 * 1024.0)

            return when {
                gb < 4.0 ->
                    ModelCatalog.fast

                gb >= 8.0 && profile.cpuCores >= 6 ->
                    ModelCatalog.accurate

                else ->
                    ModelCatalog.balanced
            }
        }
    }
}
