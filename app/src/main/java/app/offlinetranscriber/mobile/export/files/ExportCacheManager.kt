package app.offlinetranscriber.mobile.export.files

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class ExportCacheManager(
    private val context: Context
) {

    private val workDir: File
        get() = File(context.cacheDir, "export_work").apply { if (!exists()) mkdirs() }

    private val shareDir: File
        get() = File(context.cacheDir, "export_share").apply { if (!exists()) mkdirs() }

    fun createWorkFile(fileName: String): File {
        val uniqueName = "${UUID.randomUUID()}_$fileName"
        return File(workDir, uniqueName)
    }

    fun stageForShare(sourceFile: File, targetFileName: String): File {
        val target = File(shareDir, targetFileName)
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
        return target
    }

    fun cleanWorkFiles() {
        workDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    fun cleanShareFiles(olderThanMs: Long = 24 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        shareDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
