package com.example.transcriber.export

import com.example.transcriber.export.document.ExportBlock
import com.example.transcriber.export.document.ExportDocument
import com.example.transcriber.export.document.MetadataItem
import com.example.transcriber.export.model.ExportFormat
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.render.DocxExportRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class DocxExportRendererTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `renders valid docx zip package containing required ooxml files`() = runBlocking {
        val document = ExportDocument(
            title = "Quarterly Review",
            subtitle = "Executive Briefing",
            metadata = listOf(
                MetadataItem("Author", "Offline Transcriber"),
                MetadataItem("Date", "Sept 2026")
            ),
            blocks = listOf(
                ExportBlock.Heading(1, "Overview"),
                ExportBlock.Paragraph("All deliverables achieved ahead of schedule."),
                ExportBlock.TranscriptSegment("Revenue exceeded forecast by 25%.", 10000L, "Speaker 1"),
                ExportBlock.Checklist(true, "Publish investor report", "Finance", 20000L),
                ExportBlock.KeyValue("Conclusion", "Strong performance")
            )
        )

        val targetFile = tempFolder.newFile("export.docx")
        val renderer = DocxExportRenderer()
        val written = renderer.render(document, ExportOptions(format = ExportFormat.DOCX), targetFile)

        assertTrue(written > 0)
        assertTrue(targetFile.exists())

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(FileInputStream(targetFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("word/_rels/document.xml.rels"))
        assertTrue(entries.containsKey("word/styles.xml"))
        assertTrue(entries.containsKey("word/document.xml"))

        val docXml = String(entries["word/document.xml"]!!)
        assertTrue(docXml.contains("Quarterly Review"))
        assertTrue(docXml.contains("Executive Briefing"))
        assertTrue(docXml.contains("Revenue exceeded forecast by 25%."))
        assertTrue(docXml.contains("Publish investor report"))
        assertTrue(docXml.contains("Strong performance"))
    }
}
