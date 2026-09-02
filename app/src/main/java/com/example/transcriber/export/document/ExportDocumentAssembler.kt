package com.example.transcriber.export.document

import com.example.transcriber.export.format.ExportTimestampFormatter
import com.example.transcriber.export.model.ExportContentType
import com.example.transcriber.export.model.ExportOptions
import com.example.transcriber.export.model.ExportTarget
import com.example.transcriber.export.source.ExportSnapshotProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportDocumentAssembler(
    private val provider: ExportSnapshotProvider
) {

    suspend fun assemble(
        target: ExportTarget,
        options: ExportOptions
    ): ExportDocument {
        return when (target.contentType) {
            ExportContentType.TRANSCRIPT -> assembleTranscript(target.sourceId, options)
            ExportContentType.MEETING_PACK -> assembleMeeting(target.sourceId, options)
            ExportContentType.STUDY_PACK -> assembleStudy(target.sourceId, options)
            ExportContentType.ASK_CONVERSATION -> assembleAsk(target.sourceId, options)
        }
    }

    private suspend fun assembleTranscript(
        transcriptId: Long,
        options: ExportOptions
    ): ExportDocument {
        val snapshot = provider.transcript(
            transcriptId = transcriptId,
            includeSpeakerLabels = options.includeSpeakerLabels
        )

        val metadata = if (options.includeMetadata) {
            val dateStr = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(snapshot.createdAt))
            listOf(
                MetadataItem("Duration", ExportTimestampFormatter.format(snapshot.durationMs)),
                MetadataItem("Language", snapshot.languageLabel ?: "Auto"),
                MetadataItem("Date", dateStr),
                MetadataItem("Media Type", snapshot.mediaType)
            )
        } else {
            emptyList()
        }

        val blocks = mutableListOf<ExportBlock>()
        snapshot.segments.forEach { segment ->
            blocks.add(
                ExportBlock.TranscriptSegment(
                    text = segment.text,
                    timestampMs = if (options.includeTimestamps) segment.startMs else null,
                    speakerLabel = if (options.includeSpeakerLabels) segment.speakerLabel else null
                )
            )
        }

        return ExportDocument(
            title = snapshot.title,
            subtitle = "Transcript",
            metadata = metadata,
            blocks = blocks
        )
    }

    private suspend fun assembleMeeting(
        transcriptId: Long,
        options: ExportOptions
    ): ExportDocument {
        val snapshot = provider.meeting(transcriptId)

        val metadata = if (options.includeMetadata) {
            listOf(MetadataItem("Source Transcript", snapshot.title))
        } else {
            emptyList()
        }

        val blocks = mutableListOf<ExportBlock>()

        blocks.add(ExportBlock.Heading(1, "Executive Summary"))
        blocks.add(ExportBlock.Paragraph(snapshot.summary))
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Action Items"))
        if (snapshot.actions.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No action items recorded."))
        } else {
            snapshot.actions.forEach { action ->
                val detailParts = mutableListOf<String>()
                action.assignee?.let { detailParts.add("Assignee: $it") }
                action.due?.let { detailParts.add("Due: $it") }
                val detail = if (detailParts.isNotEmpty()) detailParts.joinToString(" • ") else null

                blocks.add(
                    ExportBlock.Checklist(
                        checked = action.done,
                        text = action.text,
                        detail = detail,
                        timestampMs = if (options.includeTimestamps) action.startMs else null
                    )
                )
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Key Decisions"))
        if (snapshot.decisions.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No decisions recorded."))
        } else {
            snapshot.decisions.forEach { decision ->
                blocks.add(
                    ExportBlock.Bullet(
                        text = decision.text,
                        timestampMs = if (options.includeTimestamps) decision.startMs else null
                    )
                )
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Open Questions"))
        if (snapshot.questions.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No open questions recorded."))
        } else {
            snapshot.questions.forEach { question ->
                blocks.add(
                    ExportBlock.Bullet(
                        text = question.text,
                        timestampMs = if (options.includeTimestamps) question.startMs else null
                    )
                )
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Topics Discussed"))
        if (snapshot.topics.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No topics recorded."))
        } else {
            snapshot.topics.forEach { topic ->
                blocks.add(
                    ExportBlock.Bullet(
                        text = topic.text,
                        timestampMs = if (options.includeTimestamps) topic.startMs else null
                    )
                )
            }
        }

        return ExportDocument(
            title = snapshot.title,
            subtitle = "Meeting Intelligence Summary",
            metadata = metadata,
            blocks = blocks
        )
    }

    private suspend fun assembleStudy(
        transcriptId: Long,
        options: ExportOptions
    ): ExportDocument {
        val snapshot = provider.study(transcriptId)

        val metadata = if (options.includeMetadata) {
            listOf(MetadataItem("Source Transcript", snapshot.title))
        } else {
            emptyList()
        }

        val blocks = mutableListOf<ExportBlock>()

        blocks.add(ExportBlock.Heading(1, "Key Takeaways"))
        if (snapshot.keyPoints.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No key takeaways recorded."))
        } else {
            snapshot.keyPoints.forEach { point ->
                blocks.add(ExportBlock.Bullet(text = point))
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Chapters & Outline"))
        if (snapshot.chapters.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No chapters recorded."))
        } else {
            snapshot.chapters.forEach { chapter ->
                val timeSuffix = if (options.includeTimestamps) " (${ExportTimestampFormatter.format(chapter.startMs)})" else ""
                blocks.add(ExportBlock.Heading(2, chapter.title + timeSuffix))
                blocks.add(ExportBlock.Paragraph(chapter.summary))
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Flashcards"))
        if (snapshot.cards.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No flashcards recorded."))
        } else {
            snapshot.cards.forEach { card ->
                blocks.add(ExportBlock.KeyValue("Q: ${card.question}", "A: ${card.answer}"))
            }
        }
        blocks.add(ExportBlock.Divider)

        blocks.add(ExportBlock.Heading(1, "Practice Quiz"))
        if (snapshot.quiz.isEmpty()) {
            blocks.add(ExportBlock.Paragraph("No quiz questions recorded."))
        } else {
            snapshot.quiz.forEachIndexed { index, quizItem ->
                blocks.add(ExportBlock.Heading(2, "Question ${index + 1}: ${quizItem.question}"))
                quizItem.options.forEachIndexed { optIdx, optText ->
                    val letter = ('A'.code + optIdx).toChar()
                    blocks.add(ExportBlock.Paragraph("$letter. $optText"))
                }
                if (options.includeQuizAnswers) {
                    val correctLetter = ('A'.code + quizItem.correctIndex).toChar()
                    val correctText = quizItem.options.getOrNull(quizItem.correctIndex) ?: ""
                    blocks.add(ExportBlock.KeyValue("Answer", "$correctLetter: $correctText"))
                    if (quizItem.explanation.isNotBlank()) {
                        blocks.add(ExportBlock.Paragraph("Explanation: ${quizItem.explanation}"))
                    }
                }
                blocks.add(ExportBlock.Spacer)
            }
        }

        return ExportDocument(
            title = snapshot.title,
            subtitle = "Study Pack",
            metadata = metadata,
            blocks = blocks
        )
    }

    private suspend fun assembleAsk(
        conversationId: Long,
        options: ExportOptions
    ): ExportDocument {
        val snapshot = provider.ask(conversationId)

        val metadata = if (options.includeMetadata) {
            listOf(
                MetadataItem("Scope", snapshot.scope),
                MetadataItem("Total Messages", snapshot.messages.size.toString())
            )
        } else {
            emptyList()
        }

        val blocks = mutableListOf<ExportBlock>()
        snapshot.messages.forEach { msg ->
            val speaker = if (msg.role.equals("USER", ignoreCase = true)) "You" else "Offline AI"
            blocks.add(ExportBlock.Heading(2, speaker))
            blocks.add(ExportBlock.Paragraph(msg.text))

            if (options.includeAskCitations && msg.citations.isNotEmpty()) {
                val citationText = msg.citations.joinToString("\n") { citation ->
                    "• ${citation.transcriptTitle} (${ExportTimestampFormatter.format(citation.startMs)})"
                }
                blocks.add(ExportBlock.KeyValue("Citations", citationText))
            }
            blocks.add(ExportBlock.Spacer)
        }

        return ExportDocument(
            title = snapshot.title,
            subtitle = "Ask AI Conversation",
            metadata = metadata,
            blocks = blocks
        )
    }
}
