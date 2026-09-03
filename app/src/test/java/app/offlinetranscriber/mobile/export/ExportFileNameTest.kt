package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.export.files.ExportFileName
import app.offlinetranscriber.mobile.export.model.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFileNameTest {

    @Test
    fun `sanitizes illegal file system characters`() {
        val title = "Q1 Review: <Strategy> & \"Growth\" / Results? | 2026*"
        val fileName = ExportFileName.create(title, ExportFormat.PDF)
        assertEquals("Q1 Review Strategy & Growth Results 2026.pdf", fileName)
    }

    @Test
    fun `handles blank title with fallback`() {
        val fileName = ExportFileName.create("   ", ExportFormat.DOCX)
        assertEquals("Offline Transcriber.docx", fileName)
    }

    @Test
    fun `appends suffix when provided`() {
        val fileName = ExportFileName.create("Sprint 42", ExportFormat.MARKDOWN, "Meeting")
        assertEquals("Sprint 42 - Meeting.md", fileName)
    }

    @Test
    fun `caps long titles to prevent filesystem errors`() {
        val longTitle = "A".repeat(150)
        val fileName = ExportFileName.create(longTitle, ExportFormat.TXT)
        assertTrue(fileName.length <= 85)
        assertTrue(fileName.endsWith(".txt"))
    }
}
