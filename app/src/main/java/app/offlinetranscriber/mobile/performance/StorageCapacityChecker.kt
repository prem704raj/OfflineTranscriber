package app.offlinetranscriber.mobile.performance

import android.content.Context
import android.os.StatFs

sealed interface CapacityResult {
    data object Enough : CapacityResult

    data class NotEnough(
        val requiredBytes: Long,
        val availableBytes: Long
    ) : CapacityResult
}

class StorageCapacityChecker(
    private val context: Context
) {
    companion object {
        const val SAFETY_BYTES =
            100L * 1024L * 1024L
    }

    fun checkInternal(
        expectedAdditionalBytes: Long
    ): CapacityResult {
        val stat =
            StatFs(context.filesDir.absolutePath)

        val available = stat.availableBytes
        val required =
            expectedAdditionalBytes.coerceAtLeast(0L) +
                SAFETY_BYTES

        return if (available >= required) {
            CapacityResult.Enough
        } else {
            CapacityResult.NotEnough(
                required,
                available
            )
        }
    }
}
