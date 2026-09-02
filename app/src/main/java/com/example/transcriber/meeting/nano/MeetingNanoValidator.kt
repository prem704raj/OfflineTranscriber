package com.example.transcriber.meeting.nano

import com.example.transcriber.meeting.MeetingChunk
import com.example.transcriber.meeting.MeetingSourceFormatter
import com.example.transcriber.meeting.model.GeneratedMeetingAction
import com.example.transcriber.meeting.model.GeneratedMeetingDecision
import com.example.transcriber.meeting.model.GeneratedMeetingPack
import com.example.transcriber.meeting.model.GeneratedMeetingQuestion
import com.example.transcriber.meeting.model.GeneratedMeetingSummary
import com.example.transcriber.meeting.model.GeneratedMeetingTopic
import com.example.transcriber.meeting.model.MeetingEngine
import java.util.Locale

object MeetingNanoValidator {

    fun validateProtocol(
        parsed: ParsedProtocolResult,
        chunk: MeetingChunk,
        labeledChunk: MeetingSourceFormatter.LabeledChunk,
        engine: MeetingEngine = MeetingEngine.GEMINI_NANO_PROTOCOL
    ): GeneratedMeetingPack {
        val segmentMap = chunk.segments.associateBy { it.segmentId }
        val labelToSegmentId = labeledChunk.labelToSegment

        // Summary citations
        val validSummarySources = parsed.summarySources
            .mapNotNull { labelToSegmentId[it.uppercase()] }
            .distinct()
            .take(4)

        // Actions
        val actions = mutableListOf<GeneratedMeetingAction>()
        val seenActionTexts = mutableSetOf<String>()

        parsed.actions.forEach { action ->
            val segId = labelToSegmentId[action.sourceLabel.uppercase()] ?: return@forEach
            val segment = segmentMap[segId] ?: return@forEach
            val normText = normalize(action.text)
            if (normText.isBlank() || !seenActionTexts.add(normText)) return@forEach

            val validAssignee = explicitOrEmpty(action.assignee, segment.text)
            val validDue = explicitOrEmpty(action.dueText, segment.text)

            actions.add(
                GeneratedMeetingAction(
                    text = action.text.trim(),
                    assignee = validAssignee,
                    dueText = validDue,
                    sourceSegmentId = segId,
                    startMs = segment.startMs
                )
            )
        }

        // Decisions
        val decisions = mutableListOf<GeneratedMeetingDecision>()
        val seenDecisions = mutableSetOf<String>()

        parsed.decisions.forEach { decision ->
            val segId = labelToSegmentId[decision.sourceLabel.uppercase()] ?: return@forEach
            val segment = segmentMap[segId] ?: return@forEach
            val normText = normalize(decision.text)
            if (normText.isBlank() || !seenDecisions.add(normText)) return@forEach

            decisions.add(
                GeneratedMeetingDecision(
                    text = decision.text.trim(),
                    sourceSegmentId = segId,
                    startMs = segment.startMs
                )
            )
        }

        // Questions
        val questions = mutableListOf<GeneratedMeetingQuestion>()
        val seenQuestions = mutableSetOf<String>()

        parsed.questions.forEach { question ->
            val segId = labelToSegmentId[question.sourceLabel.uppercase()] ?: return@forEach
            val segment = segmentMap[segId] ?: return@forEach
            val normText = normalize(question.text)
            if (normText.isBlank() || !seenQuestions.add(normText)) return@forEach

            questions.add(
                GeneratedMeetingQuestion(
                    text = question.text.trim(),
                    sourceSegmentId = segId,
                    startMs = segment.startMs
                )
            )
        }

        // Topics
        val topics = mutableListOf<GeneratedMeetingTopic>()
        val seenTopics = mutableSetOf<String>()

        parsed.topics.forEach { topic ->
            val segId = labelToSegmentId[topic.sourceLabel.uppercase()] ?: return@forEach
            val segment = segmentMap[segId] ?: return@forEach
            val normTitle = normalize(topic.title)
            if (normTitle.isBlank() || !seenTopics.add(normTitle)) return@forEach

            topics.add(
                GeneratedMeetingTopic(
                    title = topic.title.trim(),
                    sourceSegmentId = segId,
                    startMs = segment.startMs
                )
            )
        }

        return GeneratedMeetingPack(
            summary = GeneratedMeetingSummary(
                text = parsed.summary.trim(),
                sourceSegmentIds = if (validSummarySources.isNotEmpty()) validSummarySources
                else chunk.segments.firstOrNull()?.let { listOf(it.segmentId) } ?: emptyList()
            ),
            actions = actions.take(8),
            decisions = decisions.take(6),
            questions = questions.take(6),
            topics = topics.sortedBy { it.startMs }.take(6),
            engine = engine
        )
    }

    private fun explicitOrEmpty(
        candidate: String,
        source: String
    ): String {
        val c = normalize(candidate)
        if (c.isBlank()) return ""

        val s = normalize(source)
        val supported = s.contains(c) ||
            c.split(" ")
                .filter { it.length >= 3 }
                .any { s.contains(it) }

        return if (supported) {
            candidate.trim().take(80)
        } else {
            ""
        }
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
}
