package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.export.document.ExportBlock
import app.offlinetranscriber.mobile.export.document.ExportDocument
import app.offlinetranscriber.mobile.export.document.MetadataItem
import app.offlinetranscriber.mobile.export.model.ExportFormat
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.render.TxtExportRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TxtExportRendererTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `renders complete txt document structure`() = runBlocking {
        val document = ExportDocument(
            title = "Test Transcript",
            subtitle = "Full Recording",
            metadata = listOf(
                MetadataItem("Duration", "05:30"),
                MetadataItem("Language", "English")
            ),
            blocks = listOf(
                ExportBlock.Heading(1, "Main Section"),
                ExportBlock.Paragraph("This is an introductory paragraph."),
                ExportBlock.TranscriptSegment("Hello team, welcome to the sync.", 0L, "Alice"),
                ExportBlock.TranscriptSegment("Glad to be here!", 15000L, "Bob"),
                ExportBlock.Checklist(true, "Finalize architecture", "Alice", 30000L),
                ExportBlock.Checklist(false, "Run load tests", null, null),
                ExportBlock.Bullet("Key item 1", 45000L),
                ExportBlock.KeyValue("Status", "Complete")
            )
        )

        val targetFile = tempFolder.newFile("export.txt")
        val renderer = TxtExportRenderer()
        val written = renderer.render(document, ExportOptions(format = ExportFormat.TXT), targetFile)

        assertTrue(written > 0)
        val content = targetFile.readText()

        assertTrue(content.contains("TEST TRANSCRIPT"))
        assertTrue(content.contains("Full Recording"))
        assertTrue(content.contains("Duration: 05:30"))
        assertTrue(content.contains("MAIN SECTION"))
        assertTrue(content.contains("[0:00] [Alice] Hello team, welcome to the sync."))
        assertTrue(content.contains("[0:15] [Bob] Glad to be here!"))
        assertTrue(content.contains("[X] Finalize architecture (Alice) [0:30]"))
        assertTrue(content.contains("[ ] Run load tests"))
        assertTrue(content.contains("• [0:45] Key item 1"))
        assertTrue(content.contains("Status: Complete"))
    }
}
