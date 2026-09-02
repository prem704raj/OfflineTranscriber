package com.example.transcriber.meeting

import com.example.transcriber.meeting.model.GeneratedMeetingAction
import com.example.transcriber.meeting.model.GeneratedMeetingPack
import com.example.transcriber.meeting.model.GeneratedMeetingSummary
import com.example.transcriber.meeting.model.MeetingEngine
import java.util.Locale

object MeetingPackMerger {

    fun merge(
        enhanced: List<GeneratedMeetingPack>,
        classic: GeneratedMeetingPack
    ): GeneratedMeetingPack {
        if (enhanced.isEmpty()) return classic

        val summary = enhanced
            .map { it.summary.text }
            .filter { it.isNotBlank() }
            .distinctBy(::normalize)
            .take(5)
            .joinToString(" ")
            .take(1_500)
            .ifBlank { classic.summary.text }

        val summarySources = enhanced
            .flatMap { it.summary.sourceSegmentIds }
            .distinct()
            .take(8)
            .ifEmpty { classic.summary.sourceSegmentIds }

        val actions = dedupeActions(
            enhanced.flatMap { it.actions }
        )
            .take(16)
            .ifEmpty { classic.actions }

        val decisions = dedupe(
            enhanced.flatMap { it.decisions }
        ) { it.text }
            .take(12)
            .ifEmpty { classic.decisions }

        val questions = dedupe(
            enhanced.flatMap { it.questions }
        ) { it.text }
            .take(12)
            .ifEmpty { classic.questions }

        val topics = dedupe(
            enhanced.flatMap { it.topics }.sortedBy { it.startMs }
        ) { it.title }
            .take(12)
            .ifEmpty { classic.topics }

        val engine = when {
            enhanced.any { it.engine == MeetingEngine.GEMINI_NANO_STRUCTURED } ->
                MeetingEngine.GEMINI_NANO_STRUCTURED
            enhanced.any { it.engine == MeetingEngine.GEMINI_NANO_PROTOCOL } ->
                MeetingEngine.GEMINI_NANO_PROTOCOL
            else -> MeetingEngine.CLASSIC
        }

        return GeneratedMeetingPack(
            summary = GeneratedMeetingSummary(
                text = summary,
                sourceSegmentIds = summarySources
            ),
            actions = actions,
            decisions = decisions,
            questions = questions,
            topics = topics,
            engine = engine
        )
    }

    private fun <T> dedupe(
        input: List<T>,
        text: (T) -> String
    ): List<T> {
        val result = mutableListOf<T>()

        input.forEach { value ->
            if (result.none { similarity(text(it), text(value)) >= 0.72 }) {
                result += value
            }
        }

        return result
    }

    private fun dedupeActions(
        input: List<GeneratedMeetingAction>
    ) = dedupe(
        input.sortedBy { it.startMs }
    ) { it.text }

    private fun similarity(
        a: String,
        b: String
    ): Double {
        val one = tokenSet(a)
        val two = tokenSet(b)

        if (one.isEmpty() || two.isEmpty()) return 0.0

        return one.intersect(two).size.toDouble() / one.union(two).size.toDouble()
    }

    private fun tokenSet(
        value: String
    ) = Regex("""[\p{L}\p{N}]{2,}""")
        .findAll(value.lowercase(Locale.ROOT))
        .map { it.value }
        .toSet()

    private fun normalize(
        value: String
    ) = value.lowercase(Locale.ROOT)
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .trim()
}
