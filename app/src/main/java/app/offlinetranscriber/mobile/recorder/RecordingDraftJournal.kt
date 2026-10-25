package app.offlinetranscriber.mobile.recorder

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable
data class RecordingDraftMetadata(
    val filePath: String,
    val startedAtMs: Long,
    val durationMs: Long,
    val status: String,
    val lastHeartbeatMs: Long,
    val errorMessage: String? = null
)

object RecordingDraftJournal {
    private const val TAG = "RecordingDraftJournal"
    private const val JOURNAL_FILE_NAME = "active_recording_draft.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private fun safeWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Throwable) {
            // android.util.Log is not available under plain JVM unit tests.
        }
    }

    private fun journalFile(baseDir: File): File =
        File(baseDir, JOURNAL_FILE_NAME)

    @Synchronized
    fun start(baseDir: File, filePath: String, startedAtMs: Long) {
        val draft = RecordingDraftMetadata(
            filePath = filePath,
            startedAtMs = startedAtMs,
            durationMs = 0L,
            status = "RECORDING",
            lastHeartbeatMs = System.currentTimeMillis()
        )
        writeAtomic(baseDir, draft)
    }

    fun start(context: Context, filePath: String, startedAtMs: Long) =
        start(context.filesDir, filePath, startedAtMs)

    @Synchronized
    fun updateHeartbeat(baseDir: File, durationMs: Long, isPaused: Boolean) {
        val current = read(baseDir) ?: return
        if (current.status == "COMPLETED" || current.status == "DISCARDED") return
        val updated = current.copy(
            durationMs = durationMs,
            status = if (isPaused) "PAUSED" else "RECORDING",
            lastHeartbeatMs = System.currentTimeMillis()
        )
        writeAtomic(baseDir, updated)
    }

    fun updateHeartbeat(context: Context, durationMs: Long, isPaused: Boolean) =
        updateHeartbeat(context.filesDir, durationMs, isPaused)

    @Synchronized
    fun complete(baseDir: File, durationMs: Long) {
        clear(baseDir)
    }

    fun complete(context: Context, durationMs: Long) =
        complete(context.filesDir, durationMs)

    @Synchronized
    fun markInterrupted(baseDir: File, filePath: String?, durationMs: Long, reason: String) {
        val path = filePath ?: read(baseDir)?.filePath ?: return
        val draft = RecordingDraftMetadata(
            filePath = path,
            startedAtMs = System.currentTimeMillis() - durationMs,
            durationMs = durationMs,
            status = "INTERRUPTED",
            lastHeartbeatMs = System.currentTimeMillis(),
            errorMessage = reason
        )
        writeAtomic(baseDir, draft)
    }

    fun markInterrupted(context: Context, filePath: String?, durationMs: Long, reason: String) =
        markInterrupted(context.filesDir, filePath, durationMs, reason)

    @Synchronized
    fun discard(baseDir: File) = discardOwned(baseDir, null)

    fun discard(context: Context) =
        discardOwned(context.filesDir, externalRecordingRoot(context))

    @Synchronized
    private fun discardOwned(baseDir: File, externalRoot: File?) {
        val current = read(baseDir)
        if (current != null) {
            // Journal content can be corrupted; never delete an arbitrary stored path.
            val ownedFile = resolveOwnedAudioFile(baseDir, current.filePath, externalRoot)
            if (ownedFile == null) {
                safeWarn("Ignoring untrusted recording draft deletion path: ${current.filePath}")
            } else {
                runCatching { if (ownedFile.exists()) ownedFile.delete() }
            }
        }
        clear(baseDir)
    }

    private fun externalRecordingRoot(context: Context): File? =
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?.let { File(it, "recordings") }

    private fun resolveOwnedAudioFile(
        baseDir: File,
        path: String,
        externalRoot: File?
    ): File? = runCatching {
        val file = File(path).canonicalFile
        val roots = listOfNotNull(baseDir, externalRoot).map { it.canonicalFile }
        val underAppRoot = roots.any { root ->
            file.path.startsWith(root.path + File.separator)
        }
        file.takeIf {
            underAppRoot && (file.name.endsWith(".wav", ignoreCase = true) ||
                file.name.endsWith(".m4a", ignoreCase = true))
        }
    }.getOrNull()

    @Synchronized
    fun clear(baseDir: File) {
        runCatching {
            val file = journalFile(baseDir)
            if (file.exists()) file.delete()
        }
    }

    fun clear(context: Context) =
        clear(context.filesDir)

    @Synchronized
    fun read(baseDir: File): RecordingDraftMetadata? {
        val file = journalFile(baseDir)
        if (!file.exists() || file.length() == 0L) return null
        return runCatching {
            json.decodeFromString<RecordingDraftMetadata>(file.readText())
        }.getOrNull()
    }

    fun read(context: Context): RecordingDraftMetadata? =
        read(context.filesDir)

    @Synchronized
    fun getRecoverableDraft(baseDir: File): RecordingDraftMetadata? =
        findRecoverableDraft(baseDir, null)

    private fun findRecoverableDraft(
        baseDir: File,
        externalRoot: File?
    ): RecordingDraftMetadata? {
        val draft = read(baseDir) ?: return null
        if (draft.status in listOf("RECORDING", "PAUSED", "INTERRUPTED")) {
            val audioFile = resolveOwnedAudioFile(baseDir, draft.filePath, externalRoot)
                ?: return null
            val isWav = audioFile.name.endsWith(".wav", ignoreCase = true)
            if (audioFile.exists() && audioFile.length() > WavAudioWriter.HEADER_SIZE) {
                if (isWav) {
                    // Only repair/seal WAV files with WavAudioWriter
                    WavAudioWriter.repairWavHeader(audioFile)
                    if (WavAudioWriter.isValidWav(audioFile)) {
                        val actualDur = ((audioFile.length() - WavAudioWriter.HEADER_SIZE) * 1000L) /
                            (WavAudioWriter.SAMPLE_RATE_16K * WavAudioWriter.CHANNELS_MONO * WavAudioWriter.BYTES_PER_SAMPLE)
                        return draft.copy(durationMs = maxOf(draft.durationMs, actualDur))
                    }
                } else {
                    // Legacy M4A or other audio format - verify it has audio bytes without overwriting header
                    if (isValidM4a(audioFile)) {
                        return draft
                    }
                }
            }
        }
        return null
    }

    fun isValidM4a(file: File): Boolean {
        if (!file.exists() || file.length() < 32L) return false
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val buf = ByteArray(12)
                val read = raf.read(buf)
                if (read < 8) return false
                val boxSize = ((buf[0].toLong() and 0xFF) shl 24) or
                    ((buf[1].toLong() and 0xFF) shl 16) or
                    ((buf[2].toLong() and 0xFF) shl 8) or
                    (buf[3].toLong() and 0xFF)
                val hasFtyp = buf[4] == 'f'.code.toByte() &&
                    buf[5] == 't'.code.toByte() &&
                    buf[6] == 'y'.code.toByte() &&
                    buf[7] == 'p'.code.toByte()
                val plausibleBoxSize = boxSize == 0L || (boxSize >= 8L && boxSize <= file.length())
                hasFtyp && plausibleBoxSize
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getRecoverableDraft(context: Context): RecordingDraftMetadata? =
        findRecoverableDraft(context.filesDir, externalRecordingRoot(context))

    private fun writeAtomic(baseDir: File, draft: RecordingDraftMetadata) {
        runCatching {
            baseDir.mkdirs()
            val finalFile = journalFile(baseDir)
            val tempFile = File(baseDir, "$JOURNAL_FILE_NAME.tmp")
            val bytes = json.encodeToString(draft).toByteArray(Charsets.UTF_8)
            FileOutputStream(tempFile).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            // Atomic replacement preserves the previous journal if the process
            // dies between writing and renaming. Never delete the old journal first.
            try {
                Files.move(
                    tempFile.toPath(),
                    finalFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(
                    tempFile.toPath(),
                    finalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }.onFailure { Log.e(TAG, "Failed to write recording journal", it) }
    }
}
