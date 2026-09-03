package app.offlinetranscriber.mobile.export

import app.offlinetranscriber.mobile.export.document.ExportBlock
import app.offlinetranscriber.mobile.export.document.ExportDocumentAssembler
import app.offlinetranscriber.mobile.export.model.ExportContentType
import app.offlinetranscriber.mobile.export.model.ExportFormat
import app.offlinetranscriber.mobile.export.model.ExportOptions
import app.offlinetranscriber.mobile.export.model.ExportTarget
import app.offlinetranscriber.mobile.export.source.AskCitationExport
import app.offlinetranscriber.mobile.export.source.AskExportSnapshot
import app.offlinetranscriber.mobile.export.source.AskMessageExport
import app.offlinetranscriber.mobile.export.source.ExportSnapshotProvider
import app.offlinetranscriber.mobile.export.source.MeetingExportAction
import app.offlinetranscriber.mobile.export.source.MeetingExportItem
import app.offlinetranscriber.mobile.export.source.MeetingExportSnapshot
import app.offlinetranscriber.mobile.export.source.StudyCardExport
import app.offlinetranscriber.mobile.export.source.StudyChapterExport
import app.offlinetranscriber.mobile.export.source.StudyExportSnapshot
import app.offlinetranscriber.mobile.export.source.StudyQuizExport
import app.offlinetranscriber.mobile.export.source.TranscriptExportSegment
import app.offlinetranscriber.mobile.export.source.TranscriptExportSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportDocumentAssemblerTest {

    private val fakeProvider = object : ExportSnapshotProvider {
        override suspend fun transcript(transcriptId: Long, includeSpeakerLabels: Boolean): TranscriptExportSnapshot {
            return TranscriptExportSnapshot(
                id = transcriptId,
                title = "Team Standup",
                durationMs = 120_000L,
                createdAt = 1770000000000L,
                mediaType = "AUDIO",
                languageLabel = "English",
                segments = listOf(
                    TranscriptExportSegment(1L, 0L, 5000L, "Hello world", if (includeSpeakerLabels) "Speaker 1" else null),
                    TranscriptExportSegment(2L, 5000L, 10000L, "Status update here", if (includeSpeakerLabels) "Speaker 2" else null)
                )
            )
        }

        override suspend fun meeting(transcriptId: Long): MeetingExportSnapshot {
            return MeetingExportSnapshot(
                transcriptId = transcriptId,
                title = "Design Sync",
                summary = "Reviewed Phase 15 export architecture.",
                actions = listOf(
                    MeetingExportAction("Write unit tests", false, "Engineer", "Today", 15000L)
                ),
                decisions = listOf(
                    MeetingExportItem("Use native Android graphics and streaming zip", 30000L)
                ),
                questions = listOf(
                    MeetingExportItem("Support A4 and Letter?", 45000L)
                ),
                topics = listOf(
                    MeetingExportItem("Architecture: Export Hub", 0L)
                )
            )
        }

        override suspend fun study(transcriptId: Long): StudyExportSnapshot {
            return StudyExportSnapshot(
                transcriptId = transcriptId,
                title = "Physics 101",
                keyPoints = listOf("Newton laws", "Conservation of energy"),
                chapters = listOf(
                    StudyChapterExport("Mechanics", "Study of motion", 0L)
                ),
                cards = listOf(
                    StudyCardExport("What is F?", "m * a")
                ),
                quiz = listOf(
                    StudyQuizExport("What is gravity?", listOf("A force", "A fruit", "A wave", "A planet"), 0, "Gravity is a fundamental force.")
                )
            )
        }

        override suspend fun ask(conversationId: Long): AskExportSnapshot {
            return AskExportSnapshot(
                conversationId = conversationId,
                title = "Chat 1",
                scope = "LIBRARY",
                messages = listOf(
                    AskMessageExport("USER", "What was discussed about budget?", emptyList()),
                    AskMessageExport(
                        "ASSISTANT",
                        "The budget was increased by 10%.",
                        listOf(AskCitationExport("Financial Sync", 12000L))
                    )
                )
            )
        }
    }

    private val assembler = ExportDocumentAssembler(fakeProvider)

    @Test
    fun `assembles transcript with speaker labels and metadata`() = runBlocking {
        val target = ExportTarget(ExportContentType.TRANSCRIPT, 1L)
        val options = ExportOptions(format = ExportFormat.TXT, includeSpeakerLabels = true, includeTimestamps = true)
        val doc = assembler.assemble(target, options)

        assertEquals("Team Standup", doc.title)
        assertEquals("Transcript", doc.subtitle)
        assertTrue(doc.metadata.any { it.label == "Duration" && it.value == "2:00" })
        assertEquals(2, doc.blocks.size)

        val first = doc.blocks[0] as ExportBlock.TranscriptSegment
        assertEquals("Speaker 1", first.speakerLabel)
        assertEquals(0L, first.timestampMs)
        assertEquals("Hello world", first.text)
    }

    @Test
    fun `assembles meeting pack with sections`() = runBlocking {
        val target = ExportTarget(ExportContentType.MEETING_PACK, 1L)
        val options = ExportOptions(format = ExportFormat.MARKDOWN)
        val doc = assembler.assemble(target, options)

        assertEquals("Design Sync", doc.title)
        assertEquals("Meeting Intelligence Summary", doc.subtitle)
        assertTrue(doc.blocks.any { it is ExportBlock.Heading && it.text == "Action Items" })
        assertTrue(doc.blocks.any { it is ExportBlock.Checklist && it.text == "Write unit tests" })
        assertTrue(doc.blocks.any { it is ExportBlock.Heading && it.text == "Key Decisions" })
    }

    @Test
    fun `assembles study pack with quiz answers`() = runBlocking {
        val target = ExportTarget(ExportContentType.STUDY_PACK, 1L)
        val options = ExportOptions(format = ExportFormat.PDF, includeQuizAnswers = true)
        val doc = assembler.assemble(target, options)

        assertEquals("Physics 101", doc.title)
        assertTrue(doc.blocks.any { it is ExportBlock.Heading && it.text == "Flashcards" })
        assertTrue(doc.blocks.any { it is ExportBlock.KeyValue && it.key == "Answer" && it.value.startsWith("A:") })
    }

    @Test
    fun `assembles ask conversation with citations`() = runBlocking {
        val target = ExportTarget(ExportContentType.ASK_CONVERSATION, 1L)
        val options = ExportOptions(format = ExportFormat.DOCX, includeAskCitations = true)
        val doc = assembler.assemble(target, options)

        assertEquals("Chat 1", doc.title)
        assertTrue(doc.blocks.any { it is ExportBlock.Heading && it.text == "Offline AI" })
        assertTrue(doc.blocks.any { it is ExportBlock.KeyValue && it.key == "Citations" })
    }
}
