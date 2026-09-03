package app.offlinetranscriber.mobile.export.render

import app.offlinetranscriber.mobile.export.document.ExportDocument
import app.offlinetranscriber.mobile.export.model.ExportOptions
import java.io.File

interface ExportRenderer {

    suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long
}
