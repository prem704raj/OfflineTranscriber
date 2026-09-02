package com.example.transcriber.ask

import com.example.transcriber.ask.model.AskCitation
import com.example.transcriber.ask.model.AskEngine
import com.example.transcriber.ask.model.AskEvidence
import com.example.transcriber.ask.model.GroundedAskAnswer
import java.util.Locale

class ClassicAskEngine {

    private val stop =
        setOf(
            "the", "a", "an", "and",
            "or", "is", "are", "was",
            "were", "to", "of", "in",
            "on", "for", "what", "why",
            "how", "when", "where",
            "which", "who", "tell",
            "me", "about"
        )

    fun answer(
        question: String,
        evidence: List<AskEvidence>
    ): GroundedAskAnswer {

        if (evidence.isEmpty()) {
            return GroundedAskAnswer(
                answer = "I couldn't find enough matching evidence in your transcripts.",
                engine = AskEngine.CLASSIC,
                citations = emptyList(),
                insufficientEvidence = true
            )
        }

        val q = tokens(question)
            .filterNot(stop::contains)
            .toSet()

        val ranked = evidence
            .map { item ->
                val tokens = tokens(item.text)
                val overlap = tokens.count { it in q }
                val definition =
                    if (
                        item.text.contains(" is ", true) ||
                        item.text.contains(" means ", true) ||
                        item.text.contains(" refers to ", true)
                    ) 2
                    else 0

                item to (overlap * 3 + definition - item.rank)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy {
                it.text
                    .lowercase(Locale.ROOT)
                    .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
                    .trim()
            }
            .take(4)

        val best = ranked
            .filter { it.text.length >= 15 }
            .take(3)

        if (best.isEmpty()) {
            return GroundedAskAnswer(
                answer = "I found related sections, but not enough clear evidence to answer confidently.",
                engine = AskEngine.CLASSIC,
                citations = evidence.take(3).map(::citation),
                insufficientEvidence = true
            )
        }

        return GroundedAskAnswer(
            answer = best
                .joinToString(" ") {
                    it.text
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                }
                .take(1_000),
            engine = AskEngine.CLASSIC,
            citations = best.map(::citation),
            insufficientEvidence = false
        )
    }

    private fun citation(
        value: AskEvidence
    ) = AskCitation(
        segmentId = value.segmentId,
        transcriptId = value.transcriptId,
        transcriptTitle = value.transcriptTitle,
        mediaType = value.mediaType,
        startMs = value.startMs,
        quotePreview = value.text
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(180)
    )

    private fun tokens(
        text: String
    ): List<String> =
        Regex("""[\p{L}\p{N}]{2,}""")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .toList()
}
