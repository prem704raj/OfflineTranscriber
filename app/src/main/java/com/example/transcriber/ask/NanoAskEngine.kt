package com.example.transcriber.ask

import com.google.mlkit.genai.prompt.GenerativeModel
import com.example.transcriber.ask.model.AskCitation
import com.example.transcriber.ask.model.AskEngine
import com.example.transcriber.ask.model.AskEvidence
import com.example.transcriber.ask.model.GroundedAskAnswer

class NanoAskEngine(
    private val model: GenerativeModel
) {

    suspend fun answer(
        question: String,
        evidence: List<AskEvidence>
    ): GroundedAskAnswer {

        require(evidence.isNotEmpty())

        model.warmup()

        val map = evidence
            .take(16)
            .mapIndexed { index, item ->
                "S${index + 1}" to item
            }
            .toMap()

        val evidenceText = map.entries
            .joinToString("\n") { (label, item) ->
                "[$label] " +
                    "source=${item.transcriptTitle}; " +
                    "time=${item.startMs}; " +
                    "text=${item.text.replace("\n", " ").take(650)}"
            }

        val prompt =
            """
You answer only from the transcript evidence supplied below.

Rules:
- Use only the supplied evidence.
- Do not add outside knowledge.
- If evidence is insufficient, say so.
- Never invent a timestamp.
- Never invent a source label.
- Cite only evidence labels that directly support the answer.
- Keep the answer concise and useful.
- Return exactly:
ANSWER|your answer
CITE|S1,S2

Question:
${AskQueryNormalizer.clean(question)}

Evidence:
$evidenceText
            """.trimIndent()

        val response = model.generateContent(prompt)
        val raw = response.candidates.joinToString("\n") { it.text.orEmpty() }

        val parsed = NanoAskParser.parse(raw)
            ?: error("Invalid on-device answer.")

        var cited = parsed.citationLabels
            .mapNotNull { map[it] }
            .distinctBy { it.segmentId }

        if (cited.isEmpty()) {
            error("On-device answer had no valid evidence citation.")
        }

        // Apply diversity if multiple transcripts exist
        val uniqueTranscripts = cited.map { it.transcriptId }.distinct()
        if (uniqueTranscripts.size > 1) {
            val byTranscript = cited.groupBy { it.transcriptId }
            val diverse = mutableListOf<AskEvidence>()
            byTranscript.forEach { (_, items) ->
                diverse.addAll(items.take(3))
            }
            cited = diverse
        }

        return GroundedAskAnswer(
            answer = parsed.answer,
            engine = AskEngine.GEMINI_NANO,
            citations = cited.map { value ->
                AskCitation(
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
            }
        )
    }
}
