package app.offlinetranscriber.mobile.export.source

import app.offlinetranscriber.mobile.data.database.AppDatabase
import java.io.FileNotFoundException

class RoomExportSnapshotProvider(
    private val database: AppDatabase
) : ExportSnapshotProvider {

    private val transcriptDao = database.transcriptDao()
    private val speakerDao = database.speakerDiarizationDao()
    private val meetingDao = database.meetingDao()
    private val studyDao = database.studyDao()
    private val askDao = database.askDao()

    override suspend fun transcript(
        transcriptId: Long,
        includeSpeakerLabels: Boolean
    ): TranscriptExportSnapshot {
        val transcript = transcriptDao.getTranscriptById(transcriptId)
            ?: throw FileNotFoundException("Transcript $transcriptId not found")

        val segments = transcriptDao.getSegmentsByTranscriptId(transcriptId)

        val speakerAssignments = if (includeSpeakerLabels) {
            speakerDao.getAssignmentsOnce(transcriptId).associateBy { it.segmentId }
        } else {
            emptyMap()
        }

        val exportSegments = segments.map { segment ->
            val speakerLabel = speakerAssignments[segment.id]?.let {
                if (it.customName.isNotBlank()) it.customName else "Speaker ${it.speakerIndex + 1}"
            }
            TranscriptExportSegment(
                id = segment.id,
                startMs = segment.startMs,
                endMs = segment.endMs,
                text = segment.text,
                speakerLabel = speakerLabel
            )
        }

        return TranscriptExportSnapshot(
            id = transcript.id,
            title = transcript.title,
            durationMs = transcript.audioDurationMs,
            createdAt = transcript.createdAt,
            mediaType = transcript.mediaType,
            languageLabel = null,
            segments = exportSegments
        )
    }

    override suspend fun meeting(
        transcriptId: Long
    ): MeetingExportSnapshot {
        val transcript = transcriptDao.getTranscriptById(transcriptId)
            ?: throw FileNotFoundException("Transcript $transcriptId not found")

        val pack = meetingDao.getPack(transcriptId)
            ?: throw FileNotFoundException("Meeting pack for transcript $transcriptId not found")

        val actions = meetingDao.getActionsOnce(pack.id).map {
            MeetingExportAction(
                text = it.text,
                done = it.status.equals("COMPLETED", ignoreCase = true),
                assignee = it.assignee.takeIf { name -> name.isNotBlank() },
                due = it.dueText.takeIf { due -> due.isNotBlank() },
                startMs = it.startMs
            )
        }

        val decisions = meetingDao.getDecisionsOnce(pack.id).map {
            MeetingExportItem(
                text = it.text,
                startMs = it.startMs
            )
        }

        val questions = meetingDao.getQuestionsOnce(pack.id).map {
            MeetingExportItem(
                text = it.text,
                startMs = it.startMs
            )
        }

        val topics = meetingDao.getTopicsOnce(pack.id).map {
            MeetingExportItem(
                text = it.title,
                startMs = it.startMs
            )
        }

        return MeetingExportSnapshot(
            transcriptId = transcriptId,
            title = transcript.title,
            summary = pack.summary,
            actions = actions,
            decisions = decisions,
            questions = questions,
            topics = topics
        )
    }

    override suspend fun study(
        transcriptId: Long
    ): StudyExportSnapshot {
        val transcript = transcriptDao.getTranscriptById(transcriptId)
            ?: throw FileNotFoundException("Transcript $transcriptId not found")

        val packWithContent = studyDao.getStudyPackWithContent(transcriptId)
            ?: throw FileNotFoundException("Study pack for transcript $transcriptId not found")

        val keyPoints = packWithContent.keyPoints
            .sortedBy { it.position }
            .map { it.text }

        val chapters = packWithContent.chapters
            .sortedBy { it.position }
            .map {
                StudyChapterExport(
                    title = it.title,
                    summary = it.summary,
                    startMs = it.startMs
                )
            }

        val cards = packWithContent.flashcards
            .sortedBy { it.position }
            .map {
                StudyCardExport(
                    question = it.front,
                    answer = it.back
                )
            }

        val quiz = packWithContent.quizQuestions
            .sortedBy { it.position }
            .map {
                StudyQuizExport(
                    question = it.question,
                    options = listOf(it.optionA, it.optionB, it.optionC, it.optionD),
                    correctIndex = it.correctIndex,
                    explanation = it.explanation
                )
            }

        return StudyExportSnapshot(
            transcriptId = transcriptId,
            title = transcript.title,
            keyPoints = keyPoints,
            chapters = chapters,
            cards = cards,
            quiz = quiz
        )
    }

    override suspend fun ask(
        conversationId: Long
    ): AskExportSnapshot {
        val conversation = askDao.getConversation(conversationId)
            ?: throw FileNotFoundException("Conversation $conversationId not found")

        val messages = askDao.getMessagesOnce(conversationId)
        val allCitations = askDao.resolvedCitationsForConversation(conversationId)
            .groupBy { it.messageId }

        val exportMessages = messages.map { msg ->
            val citations = (allCitations[msg.id] ?: emptyList()).map {
                AskCitationExport(
                    transcriptTitle = it.transcriptTitle,
                    startMs = it.startMs
                )
            }
            AskMessageExport(
                role = msg.role,
                text = msg.text,
                citations = citations
            )
        }

        return AskExportSnapshot(
            conversationId = conversationId,
            title = conversation.title,
            scope = conversation.scope,
            messages = exportMessages
        )
    }
}
