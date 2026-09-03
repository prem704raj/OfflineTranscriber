package app.offlinetranscriber.mobile.backup.media

import android.content.Context
import androidx.core.net.toUri
import app.offlinetranscriber.mobile.data.database.BackupDao
import java.util.UUID

class BackupMediaPlanner(
    private val context: Context,
    private val dao: BackupDao
) {

    suspend fun plan(includeMedia: Boolean): List<BackupMediaSource> {
        if (!includeMedia) return emptyList()

        val results = mutableListOf<BackupMediaSource>()
        var afterId = Long.MIN_VALUE

        while (true) {
            val page = dao.backupTranscriptsAfter(afterId, 250)
            if (page.isEmpty()) break

            for (t in page) {
                val candidateUriString = t.sourceUri.ifBlank { t.audioUriString.orEmpty() }
                if (candidateUriString.isNotBlank()) {
                    val uri = runCatching { candidateUriString.toUri() }.getOrNull()
                    if (uri != null && isReadable(uri)) {
                        val extension = extractSafeExtension(t.audioFileName, candidateUriString)
                        val mediaId = UUID.randomUUID().toString()
                        results.add(
                            BackupMediaSource(
                                mediaId = mediaId,
                                transcriptOldId = t.id,
                                uri = uri,
                                displayName = t.audioFileName.ifBlank { "media_${t.id}.$extension" },
                                mimeType = context.contentResolver.getType(uri),
                                extension = extension
                            )
                        )
                    }
                }
                afterId = t.id
            }
        }

        return results
    }

    private fun isReadable(uri: android.net.Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun extractSafeExtension(fileName: String, uriString: String): String {
        val nameExt = fileName.substringAfterLast('.', "").lowercase()
        if (nameExt.matches(Regex("""[a-z0-9]{1,10}"""))) {
            return nameExt
        }
        val uriExt = uriString.substringAfterLast('.', "").substringBefore('?').lowercase()
        if (uriExt.matches(Regex("""[a-z0-9]{1,10}"""))) {
            return uriExt
        }
        return "bin"
    }
}
