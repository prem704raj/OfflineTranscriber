package app.offlinetranscriber.mobile.export.render

import app.offlinetranscriber.mobile.export.model.ExportFormat

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
