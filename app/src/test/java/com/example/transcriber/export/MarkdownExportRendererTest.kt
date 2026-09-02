package com.example.transcriber.export

import com.example.transcriber.export.document.ExportBlock
import com.example.transcriber.export.document.ExportDocument
import com.example.transcriber.export.document.MetadataItem
import com.example.transcriber.export.model.ExportFormat
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.render.MarkdownExportRenderer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MarkdownExportRendererTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `renders complete markdown document with formatting`() = runBlocking {
        val document = ExportDocument(
            title = "Sprint Planning",
            subtitle = "Meeting Summary",
            metadata = listOf(
                MetadataItem("Source Transcript", "Sprint Planning")
            ),
            blocks = listOf(
                ExportBlock.Heading(1, "Executive Summary"),
                ExportBlock.Paragraph("Discussed sprint goals and deliverables."),
                ExportBlock.TranscriptSegment("Let's kick off the sprint.", 0L, "Leader"),
                ExportBlock.Checklist(true, "Deploy to staging", "DevOps", 60000L),
                ExportBlock.Bullet("High priority ticket", 120000L),
                ExportBlock.KeyValue("Target Date", "Next Friday")
            )
        )

        val targetFile = tempFolder.newFile("export.md")
        val renderer = MarkdownExportRenderer()
        val written = renderer.render(document, ExportOptions(format = ExportFormat.MARKDOWN), targetFile)

        assertTrue(written > 0)
        val content = targetFile.readText()

        assertTrue(content.contains("# Sprint Planning"))
        assertTrue(content.contains("*Meeting Summary*"))
        assertTrue(content.contains("- **Source Transcript:** Sprint Planning"))
        assertTrue(content.contains("## Executive Summary"))
        assertTrue(content.contains("**`[0:00]`** **Leader:** Let's kick off the sprint."))
        assertTrue(content.contains("- [x] Deploy to staging *(DevOps)* `[1:00]`"))
        assertTrue(content.contains("- `[2:00]` High priority ticket"))
        assertTrue(content.contains("**Target Date:** Next Friday"))
    }
}
