package app.offlinetranscriber.mobile.export.files

import android.content.ContentResolver
import android.net.Uri
import app.offlinetranscriber.mobile.export.model.ExportArtifact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ExportSaver(
    private val contentResolver: ContentResolver
) {

    suspend fun saveToUri(
        artifact: ExportArtifact,
        targetUri: Uri
    ): Long = withContext(Dispatchers.IO) {
        val sourceFile = File(artifact.filePath)
        if (!sourceFile.exists()) {
            throw IOException("Export source file missing: ${artifact.filePath}")
        }

        val outputStream = contentResolver.openOutputStream(targetUri, "w")
            ?: throw IOException("Cannot open output stream for URI: $targetUri")

        outputStream.use { out ->
            FileInputStream(sourceFile).use { input ->
                input.copyTo(out)
            }
        }
        sourceFile.length()
    }
}
