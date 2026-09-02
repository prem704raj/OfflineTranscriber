package com.example.transcriber.caption.export.background

import android.content.Context
import java.io.File

class CaptionExportWorkspace(
    private val context: Context
) {

    private val workRoot = File(context.cacheDir, "caption_video_export")
    private val readyRoot = File(context.cacheDir, "caption_video_ready")
    private val shareRoot = File(context.cacheDir, "shared_caption_videos")

    fun workDir(jobId: String): File =
        File(workRoot, safeJobId(jobId)).apply { mkdirs() }

    fun readyDir(jobId: String): File =
        File(readyRoot, safeJobId(jobId)).apply { mkdirs() }

    fun shareDir(): File =
        shareRoot.apply { mkdirs() }

    fun cleanupWork(jobId: String) {
        File(workRoot, safeJobId(jobId)).deleteRecursively()
    }

    fun cleanupReady(jobId: String) {
        File(readyRoot, safeJobId(jobId)).deleteRecursively()
    }

    fun cleanupOld(
        maxAgeMs: Long = 24L * 60L * 60L * 1000L
    ) {
        val cutoff = System.currentTimeMillis() - maxAgeMs

        listOf(workRoot, readyRoot, shareRoot).forEach { root ->
            root.listFiles().orEmpty()
                .filter { it.lastModified() < cutoff }
                .forEach { it.deleteRecursively() }
        }
    }

    private fun safeJobId(value: String): String {
        require(value.matches(Regex("""[A-Za-z0-9_-]{8,80}"""))) {
            "Invalid job ID: $value"
        }
        return value
    }
}
