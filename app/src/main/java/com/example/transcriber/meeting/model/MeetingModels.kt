package com.example.transcriber.meeting.model

enum class MeetingEngine {
    CLASSIC,
    GEMINI_NANO_PROTOCOL,
    GEMINI_NANO_STRUCTURED
}

enum class MeetingActionStatus {
    OPEN,
    DONE
}

data class MeetingSourceSegment(
    val segmentId: Long,
    val transcriptId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val speakerName: String? = null,
    val speakerUserNamed: Boolean = false
)

data class GeneratedMeetingSummary(
    val text: String,
    val sourceSegmentIds: List<Long>
)

data class GeneratedMeetingAction(
    val text: String,
    val assignee: String,
    val dueText: String,
    val sourceSegmentId: Long?,
    val startMs: Long
)

data class GeneratedMeetingDecision(
    val text: String,
    val sourceSegmentId: Long?,
    val startMs: Long
)

data class GeneratedMeetingQuestion(
    val text: String,
    val sourceSegmentId: Long?,
    val startMs: Long
)

data class GeneratedMeetingTopic(
    val title: String,
    val sourceSegmentId: Long?,
    val startMs: Long
)

data class GeneratedMeetingPack(
    val summary: GeneratedMeetingSummary,
    val actions: List<GeneratedMeetingAction>,
    val decisions: List<GeneratedMeetingDecision>,
    val questions: List<GeneratedMeetingQuestion>,
    val topics: List<GeneratedMeetingTopic>,
    val engine: MeetingEngine
)
