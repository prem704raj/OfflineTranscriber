package app.offlinetranscriber.mobile.backup.restore

import android.content.Context
import java.io.File

class RestoreWorkspace(
    private val context: Context
) {

    private val root = File(context.cacheDir, "restore_sessions").apply { mkdirs() }

    fun sessionDir(sessionId: String): File =
        File(root, safe(sessionId)).apply { mkdirs() }

    fun payloadZip(sessionId: String): File =
        File(sessionDir(sessionId), "payload.zip")

    fun mediaStage(sessionId: String): File =
        File(sessionDir(sessionId), "media_stage").apply { mkdirs() }

    fun cleanup(sessionId: String) {
        sessionDir(sessionId).deleteRecursively()
    }

    fun cleanupExpired(maxAgeMs: Long = 30L * 60L * 1000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        root.listFiles().orEmpty()
            .filter { it.lastModified() < cutoff }
            .forEach { it.deleteRecursively() }
    }

    private fun safe(value: String): String {
        require(value.matches(Regex("""[A-Za-z0-9_-]{8,80}"""))) {
            "Invalid session ID format: $value"
        }
        return value
    }
}
