package com.example.transcriber.export.render

import com.example.transcriber.export.model.ExportFormat

object ExportRendererFactory {

    fun create(format: ExportFormat): ExportRenderer {
        return when (format) {
            ExportFormat.TXT -> TxtExportRenderer()
            ExportFormat.MARKDOWN -> MarkdownExportRenderer()
            ExportFormat.PDF -> PdfExportRenderer()
            ExportFormat.DOCX -> DocxExportRenderer()
        }
    }
}
