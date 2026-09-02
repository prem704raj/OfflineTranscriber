package com.example.transcriber.study.nano

import com.google.mlkit.genai.prompt.GenerativeModel
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.StudyEngine
import com.example.transcriber.study.TranscriptChunker
import com.example.transcriber.study.model.DraftChapter
import com.example.transcriber.study.model.DraftFlashcard
import com.example.transcriber.study.model.DraftQuizQuestion
import com.example.transcriber.study.model.StudyDraft
import com.example.transcriber.study.model.StudyEngineType

class NanoStudyEngine(
    private val model: GenerativeModel
) : StudyEngine {

    override suspend fun generate(
        transcript: TranscriptEntity,
        segments: List<TranscriptSegmentEntity>
    ): StudyDraft {
        require(segments.isNotEmpty()) {
            "Transcript has no segments."
        }

        model.warmup()

        val chunks = TranscriptChunker.build(segments)

        val allKeyPoints = mutableListOf<String>()
        val allChapters = mutableListOf<DraftChapter>()
        val allCards = mutableListOf<DraftFlashcard>()

        chunks.forEachIndexed { index, chunk ->
            val prompt = buildPrompt(
                title = transcript.title,
                chunkIndex = index,
                chunkCount = chunks.size,
                transcriptText = chunk.text
            )

            val response = model.generateContent(prompt)
            val raw = response.candidates.joinToString("\n") { it.text.orEmpty() }

            val parsed = NanoOutputParser.parse(
                raw = raw,
                realSegments = segments
            )

            allKeyPoints += parsed.keyPoints
            allChapters += parsed.chapters
            allCards += parsed.flashcards
        }

        val keyPoints = allKeyPoints
            .distinctBy(::canonical)
            .take(12)

        val chapters = allChapters
            .distinctBy { it.startMs }
            .sortedBy { it.startMs }
            .take(10)

        val cards = allCards
            .distinctBy {
                canonical(it.front + "|" + it.back)
            }
            .take(20)

        if (
            keyPoints.size < 3 ||
            chapters.isEmpty()
        ) {
            error("On-device AI output was incomplete.")
        }

        val quiz = buildQuiz(cards)

        return StudyDraft(
            engine = StudyEngineType.GEMINI_NANO,
            keyPoints = keyPoints,
            chapters = chapters,
            flashcards = cards,
            quizQuestions = quiz
        )
    }

    private fun buildPrompt(
        title: String,
        chunkIndex: Int,
        chunkCount: Int,
        transcriptText: String
    ): String = """
You are creating accurate study material from a transcript.

Rules:
- Use ONLY facts explicitly present in TRANSCRIPT.
- Do not invent facts.
- If information is unclear, omit it.
- Keep explanations concise.
- Timestamp values must be copied from the square brackets in TRANSCRIPT.
- Return ONLY protocol lines. No markdown, headings, code fences or commentary.

Protocol:
KP|one important factual key point
CH|timestampMs|short chapter title|one-sentence chapter summary
FC|clear question|answer grounded in transcript

Produce:
- 2 to 4 KP lines
- 1 to 3 CH lines
- 2 to 5 FC lines when enough factual material exists

Title: $title
Chunk: ${chunkIndex + 1} of $chunkCount

TRANSCRIPT:
$transcriptText
""".trimIndent()

    private fun buildQuiz(
        cards: List<DraftFlashcard>
    ): List<DraftQuizQuestion> {
        if (cards.size < 4) return emptyList()

        val answers = cards
            .map { it.back }
            .distinct()

        return cards.take(10).mapIndexedNotNull { index, card ->
            val distractors = answers
                .filter { it != card.back }
                .drop(index % maxOf(1, answers.size))
                .plus(
                    answers.filter { it != card.back }
                )
                .distinct()
                .take(3)

            if (distractors.size < 3) {
                return@mapIndexedNotNull null
            }

            val options = (
                distractors + card.back
            ).shuffled(
                java.util.Random(
                    card.front.hashCode().toLong()
                )
            )

            DraftQuizQuestion(
                question = card.front,
                options = options,
                correctIndex = options.indexOf(card.back),
                explanation = card.back
            )
        }
    }

    private fun canonical(value: String): String =
        value.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
}
