package app.offlinetranscriber.mobile.caption.export.background

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CaptionExportJobStore(
    private val context: Context
) {

    private val mutex = Mutex()
    private val file = File(context.filesDir, "caption_export_jobs.json")
    private val _jobs = MutableStateFlow<Map<String, CaptionExportJobState>>(emptyMap())
    val jobs: StateFlow<Map<String, CaptionExportJobState>> = _jobs.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!file.exists()) return
        try {
            val content = file.readText(Charsets.UTF_8)
            val list = json.decodeFromString<List<CaptionExportJobState>>(content)
            _jobs.value = list.associateBy { it.jobId }
        } catch (_: Exception) {}
    }

    private fun saveToDiskLocked() {
        try {
            val list = _jobs.value.values.take(15)
            val encoded = json.encodeToString(list)
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(encoded, Charsets.UTF_8)
            if (file.exists()) file.delete()
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
        } catch (_: Exception) {}
    }

    suspend fun put(state: CaptionExportJobState) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _jobs.value.toMutableMap()
            updated[state.jobId] = state
            _jobs.value = updated
            saveToDiskLocked()
        }
    }

    suspend fun get(jobId: String): CaptionExportJobState? = withContext(Dispatchers.IO) {
        mutex.withLock {
            _jobs.value[jobId]
        }
    }

    suspend fun latestForTranscript(transcriptId: Long): CaptionExportJobState? = withContext(Dispatchers.IO) {
        mutex.withLock {
            _jobs.value.values
                .filter { it.transcriptId == transcriptId }
                .maxByOrNull { it.updatedAt }
        }
    }

    suspend fun remove(jobId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _jobs.value.toMutableMap()
            updated.remove(jobId)
            _jobs.value = updated
            saveToDiskLocked()
        }
    }

    suspend fun cleanup(olderThanMs: Long = 24L * 60L * 60L * 1000L) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val cutoff = System.currentTimeMillis() - olderThanMs
            val filtered = _jobs.value.filterValues { it.updatedAt >= cutoff }
            _jobs.value = filtered
            saveToDiskLocked()
        }
    }
}
