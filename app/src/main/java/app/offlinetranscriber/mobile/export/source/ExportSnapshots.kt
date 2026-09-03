package app.offlinetranscriber.mobile.export.source

data class TranscriptExportSegment(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speakerLabel: String? = null
)

data class TranscriptExportSnapshot(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val createdAt: Long,
    val mediaType: String,
    val languageLabel: String?,
    val segments: List<TranscriptExportSegment>
)

data class MeetingExportAction(
    val text: String,
    val done: Boolean,
    val assignee: String?,
    val due: String?,
    val startMs: Long
)

data class MeetingExportItem(
    val text: String,
    val startMs: Long
)

data class MeetingExportSnapshot(
    val transcriptId: Long,
    val title: String,
    val summary: String,
    val actions: List<MeetingExportAction>,
    val decisions: List<MeetingExportItem>,
    val questions: List<MeetingExportItem>,
    val topics: List<MeetingExportItem>
)

data class StudyChapterExport(
    val title: String,
    val summary: String,
    val startMs: Long
)

data class StudyCardExport(
    val question: String,
    val answer: String
)

data class StudyQuizExport(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class StudyExportSnapshot(
    val transcriptId: Long,
    val title: String,
    val keyPoints: List<String>,
    val chapters: List<StudyChapterExport>,
    val cards: List<StudyCardExport>,
    val quiz: List<StudyQuizExport>
)

data class AskCitationExport(
    val transcriptTitle: String,
    val startMs: Long
)

data class AskMessageExport(
    val role: String,
    val text: String,
    val citations: List<AskCitationExport>
)

data class AskExportSnapshot(
    val conversationId: Long,
    val title: String,
    val scope: String,
    val messages: List<AskMessageExport>
)
