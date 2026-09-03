package app.offlinetranscriber.mobile.export.files

import app.offlinetranscriber.mobile.export.model.ExportArtifact
import app.offlinetranscriber.mobile.export.model.ExportFormat
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object ExportArtifactValidator {

    fun validate(artifact: ExportArtifact, format: ExportFormat) {
        val file = File(artifact.filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("Export file was not created: ${artifact.filePath}")
        }

        if (file.length() <= 0L) {
            throw IOException("Export file is empty: ${artifact.filePath}")
        }

        when (format) {
            ExportFormat.PDF -> {
                FileInputStream(file).use { input ->
                    val header = ByteArray(5)
                    val read = input.read(header)
                    if (read < 4 || String(header, 0, 4) != "%PDF") {
                        throw IOException("Generated PDF has invalid magic bytes")
                    }
                }
            }
            ExportFormat.DOCX -> {
                FileInputStream(file).use { input ->
                    val header = ByteArray(4)
                    val read = input.read(header)
                    // Zip header: 0x50 0x4B 0x03 0x04
                    if (read < 4 || header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
                        throw IOException("Generated DOCX has invalid ZIP header")
                    }
                }
            }
            ExportFormat.TXT, ExportFormat.MARKDOWN -> {
                // Verified non-empty
            }
        }
    }
}
