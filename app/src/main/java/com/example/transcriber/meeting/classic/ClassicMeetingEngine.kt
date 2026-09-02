package com.example.transcriber.meeting.classic

import com.example.transcriber.meeting.model.GeneratedMeetingAction
import com.example.transcriber.meeting.model.GeneratedMeetingDecision
import com.example.transcriber.meeting.model.GeneratedMeetingPack
import com.example.transcriber.meeting.model.GeneratedMeetingQuestion
import com.example.transcriber.meeting.model.GeneratedMeetingSummary
import com.example.transcriber.meeting.model.GeneratedMeetingTopic
import com.example.transcriber.meeting.model.MeetingEngine
import com.example.transcriber.meeting.model.MeetingSourceSegment
import java.util.Locale

class ClassicMeetingEngine {

    private val actionCue =
        Regex(
            """(?i)\b(action item|need to|needs to|have to|has to|should|must|will|please|follow up|send|prepare|finish|complete|review|check|update|share|schedule)\b"""
        )

    private val decisionCue =
        Regex(
            """(?i)\b(we decided|decided to|decision is|we agreed|agreed to|we'll go with|we will go with|final decision|finalized|approved|confirmed)\b"""
        )

    private val unresolvedCue =
        Regex(
            """(?i)\b(open question|not sure|need to find out|need to confirm|unclear|to be decided|tbd|whether we should)\b"""
        )

    private val stop =
        setOf(
            "the", "and", "that", "this",
            "with", "from", "have", "will",
            "would", "could", "should",
            "about", "there", "their",
            "they", "them", "what", "when",
            "where", "which", "your", "you",
            "our", "for", "are", "was",
            "were", "been", "being", "into",
            "than", "then", "also", "because",
            "just", "like", "okay", "right",
            "yeah", "yes", "not", "but",
            "can", "need", "going", "we",
            "to", "of", "in", "on", "is",
            "it", "or", "as"
        )

    fun generate(
        segments: List<MeetingSourceSegment>
    ): GeneratedMeetingPack {
        if (segments.isEmpty()) {
            return GeneratedMeetingPack(
                summary = GeneratedMeetingSummary(
                    text = "No transcript content is available.",
                    sourceSegmentIds = emptyList()
                ),
                actions = emptyList(),
                decisions = emptyList(),
                questions = emptyList(),
                topics = emptyList(),
                engine = MeetingEngine.CLASSIC
            )
        }

        val actions = segments
            .filter { actionCue.containsMatchIn(it.text) }
            .map {
                GeneratedMeetingAction(
                    text = clean(it.text).take(280),
                    assignee = "",
                    dueText = "",
                    sourceSegmentId = it.segmentId,
                    startMs = it.startMs
                )
            }
            .distinctBy { normalize(it.text) }
            .take(12)

        val decisions = segments
            .filter { decisionCue.containsMatchIn(it.text) }
            .map {
                GeneratedMeetingDecision(
                    text = clean(it.text).take(320),
                    sourceSegmentId = it.segmentId,
                    startMs = it.startMs
                )
            }
            .distinctBy { normalize(it.text) }
            .take(10)

        val questions = segments
            .filter { it.text.contains("?") || unresolvedCue.containsMatchIn(it.text) }
            .map {
                GeneratedMeetingQuestion(
                    text = clean(it.text).take(320),
                    sourceSegmentId = it.segmentId,
                    startMs = it.startMs
                )
            }
            .distinctBy { normalize(it.text) }
            .take(10)

        val frequency = mutableMapOf<String, Int>()

        segments.forEach { segment ->
            tokens(segment.text)
                .distinct()
                .forEach { token ->
                    frequency[token] = (frequency[token] ?: 0) + 1
                }
        }

        val sampleStep = maxOf(segments.size / 8, 1)
        val seenTopics = mutableSetOf<String>()

        val topics = segments
            .filterIndexed { index, _ -> index == 0 || index % sampleStep == 0 }
            .mapNotNull { segment ->
                val keyword = tokens(segment.text)
                    .filterNot { it in stop }
                    .maxByOrNull { frequency[it] ?: 0 }
                    ?: return@mapNotNull null

                if (!seenTopics.add(keyword)) {
                    return@mapNotNull null
                }

                GeneratedMeetingTopic(
                    title = keyword.replaceFirstChar { it.titlecase(Locale.ROOT) },
                    sourceSegmentId = segment.segmentId,
                    startMs = segment.startMs
                )
            }
            .take(8)

        val summarySources = linkedMapOf<Long, MeetingSourceSegment>()

        segments.firstOrNull()?.let {
            summarySources[it.segmentId] = it
        }

        topics.forEach { topic ->
            topic.sourceSegmentId?.let { id ->
                segments.firstOrNull { it.segmentId == id }
            }?.let {
                summarySources[it.segmentId] = it
            }
        }

        segments.lastOrNull()?.let {
            summarySources[it.segmentId] = it
        }

        val summaryText = summarySources.values
            .map { clean(it.text) }
            .filter { it.length >= 20 }
            .distinct()
            .take(5)
            .joinToString(" ")
            .take(1_200)
            .ifBlank {
                "Meeting transcript available. Open the source timeline for details."
            }

        return GeneratedMeetingPack(
            summary = GeneratedMeetingSummary(
                text = summaryText,
                sourceSegmentIds = summarySources.keys.take(5).toList()
            ),
            actions = actions,
            decisions = decisions,
            questions = questions,
            topics = topics,
            engine = MeetingEngine.CLASSIC
        )
    }

    private fun tokens(text: String): List<String> =
        Regex("""[\p{L}\p{N}]{3,}""")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .filterNot { it in stop }
            .toList()

    private fun clean(value: String) =
        value.replace(Regex("""\s+"""), " ").trim()

    private fun normalize(value: String) =
        clean(value)
            .lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
}
