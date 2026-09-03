package app.offlinetranscriber.mobile.export.render

import android.graphics.pdf.PdfDocument
import app.offlinetranscriber.mobile.export.document.ExportBlock
import app.offlinetranscriber.mobile.export.document.ExportDocument
import app.offlinetranscriber.mobile.export.format.ExportTimestampFormatter
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.render.pdf.PdfPageWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfExportRenderer : ExportRenderer {

    override suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long = withContext(Dispatchers.IO) {
        val pdfDoc = PdfDocument()
        val writer = PdfPageWriter(
            pageSize = options.pageSize,
            textScale = options.textScale
        )

        writer.start(pdfDoc)

        // Draw title & subtitle
        writer.drawTitle(document.title, document.subtitle)

        // Draw metadata
        if (document.metadata.isNotEmpty()) {
            writer.drawMetadata(document.metadata.map { it.label to it.value })
        }

        // Draw blocks
        document.blocks.forEach { block ->
            when (block) {
                is ExportBlock.Heading -> {
                    writer.drawHeading(block.level, block.text)
                }
                is ExportBlock.Paragraph -> {
                    writer.drawParagraph(block.text)
                }
                is ExportBlock.TranscriptSegment -> {
                    val time = block.timestampMs?.let { ExportTimestampFormatter.format(it) }
                    writer.drawTranscriptSegment(block.text, time, block.speakerLabel)
                }
                is ExportBlock.Bullet -> {
                    val time = block.timestampMs?.let { ExportTimestampFormatter.format(it) }
                    writer.drawBullet(block.text, time)
                }
                is ExportBlock.Checklist -> {
                    val time = block.timestampMs?.let { ExportTimestampFormatter.format(it) }
                    writer.drawChecklist(block.checked, block.text, block.detail, time)
                }
                is ExportBlock.KeyValue -> {
                    writer.drawKeyValue(block.key, block.value)
                }
                is ExportBlock.Divider -> {
                    writer.drawDivider()
                }
                is ExportBlock.Spacer -> {
                    writer.drawSpacer()
                }
            }
        }

        writer.finish()

        FileOutputStream(targetFile).use { fos ->
            pdfDoc.writeTo(fos)
        }
        pdfDoc.close()

        targetFile.length()
    }
}
