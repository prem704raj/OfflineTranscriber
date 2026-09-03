package app.offlinetranscriber.mobile.backup.estimate

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import app.offlinetranscriber.mobile.data.database.BackupDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupEstimate(
    val transcriptCount: Int,
    val estimatedDataBytes: Long,
    val mediaCount: Int,
    val estimatedMediaBytes: Long,
    val hasUnknownMediaSize: Boolean
) {
    val totalEstimatedBytes: Long
        get() = estimatedDataBytes + estimatedMediaBytes
}

class BackupSizeEstimator(
    private val context: Context,
    private val dao: BackupDao
) {

    suspend fun estimate(): BackupEstimate = withContext(Dispatchers.IO) {
        var transcriptCount = 0
        var estimatedDataBytes = 32L * 1024L // baseline overhead + manifest
        var mediaCount = 0
        var estimatedMediaBytes = 0L
        var hasUnknownMediaSize = false

        var afterId = Long.MIN_VALUE

        while (true) {
            val page = dao.backupTranscriptsAfter(afterId, 250)
            if (page.isEmpty()) break

            for (t in page) {
                transcriptCount++
                estimatedDataBytes += (t.fullText.length * 2L).coerceAtLeast(1024L)

                val uriString = t.sourceUri.ifBlank { t.audioUriString.orEmpty() }
                if (uriString.isNotBlank()) {
                    val uri = runCatching { uriString.toUri() }.getOrNull()
                    if (uri != null) {
                        val size = getMediaSize(uri)
                        if (size != null && size > 0L) {
                            mediaCount++
                            estimatedMediaBytes += size
                        } else {
                            // Rough estimation based on duration if size unavailable
                            if (t.audioDurationMs > 0L) {
                                mediaCount++
                                estimatedMediaBytes += (t.audioDurationMs / 1000L) * 16000L // ~128kbps audio
                            }
                            hasUnknownMediaSize = true
                        }
                    }
                }
                afterId = t.id
            }
        }

        BackupEstimate(
            transcriptCount = transcriptCount,
            estimatedDataBytes = estimatedDataBytes,
            mediaCount = mediaCount,
            estimatedMediaBytes = estimatedMediaBytes,
            hasUnknownMediaSize = hasUnknownMediaSize
        )
    }

    private fun getMediaSize(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.length.takeIf { it > 0 }
            }
        }.getOrNull()
    }
}
