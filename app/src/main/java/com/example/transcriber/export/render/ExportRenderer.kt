package com.example.transcriber.export.render

import com.example.transcriber.export.document.ExportDocument
import com.example.transcriber.export.model.ExportOptions
import java.io.File

interface ExportRenderer {

    suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long
}
