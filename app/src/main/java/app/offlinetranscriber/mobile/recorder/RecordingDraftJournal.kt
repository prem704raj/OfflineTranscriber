package app.offlinetranscriber.mobile.recorder

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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
    fun discard(baseDir: File) {
        val current = read(baseDir)
        if (current != null) {
            runCatching {
                val f = File(current.filePath)
                if (f.exists()) f.delete()
            }
        }
        clear(baseDir)
    }

    fun discard(context: Context) =
        discard(context.filesDir)

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
    fun getRecoverableDraft(baseDir: File): RecordingDraftMetadata? {
        val draft = read(baseDir) ?: return null
        if (draft.status in listOf("RECORDING", "PAUSED", "INTERRUPTED")) {
            val audioFile = File(draft.filePath)
            val isWav = draft.filePath.endsWith(".wav", ignoreCase = true)
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
                    return draft
                }
            }
        }
        return null
    }

    fun getRecoverableDraft(context: Context): RecordingDraftMetadata? =
        getRecoverableDraft(context.filesDir)

    private fun writeAtomic(baseDir: File, draft: RecordingDraftMetadata) {
        runCatching {
            val finalFile = journalFile(baseDir)
            val tempFile = File(baseDir, "$JOURNAL_FILE_NAME.tmp")
            tempFile.writeText(json.encodeToString(draft))
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
        }.onFailure { Log.e(TAG, "Failed to write recording journal", it) }
    }
}
