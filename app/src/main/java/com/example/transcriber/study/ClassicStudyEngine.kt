package com.example.transcriber.study

import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.model.DraftChapter
import com.example.transcriber.study.model.DraftFlashcard
import com.example.transcriber.study.model.DraftQuizQuestion
import com.example.transcriber.study.model.StudyDraft
import com.example.transcriber.study.model.StudyEngineType
import java.util.Locale
import kotlin.math.max

class ClassicStudyEngine : StudyEngine {

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then",
        "is", "are", "was", "were", "be", "been", "being",
        "to", "of", "in", "on", "for", "with", "at", "by",
        "from", "as", "that", "this", "these", "those", "it",
        "its", "we", "you", "they", "he", "she", "i", "our",
        "your", "their", "can", "could", "would", "should",
        "will", "may", "might", "do", "does", "did", "have",
        "has", "had", "not", "so", "than", "into", "about",
        "also", "there", "here", "what", "which", "when",
        "where", "how", "why", "today", "now", "going"
    )

    private val definitionPatterns = listOf(
        Regex(
            """^\s*([A-Za-z][A-Za-z0-9 _/\-]{2,45})\s+(?:is|means|refers to|is defined as)\s+(.{12,220})$""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*(?:we call|called)\s+([A-Za-z][A-Za-z0-9 _/\-]{2,45})\s+(.{12,220})$""",
            RegexOption.IGNORE_CASE
        )
    )

    override suspend fun generate(
        transcript: TranscriptEntity,
        segments: List<TranscriptSegmentEntity>
    ): StudyDraft {
        val clean = segments
            .sortedBy { it.startMs }
            .filter { it.text.isNotBlank() }

        val keyPoints = extractKeyPoints(clean)
        val chapters = buildChapters(clean)
        val flashcards = buildFlashcards(clean, keyPoints)
        val quiz = buildQuiz(flashcards)

        return StudyDraft(
            engine = StudyEngineType.CLASSIC,
            keyPoints = keyPoints,
            chapters = chapters,
            flashcards = flashcards,
            quizQuestions = quiz
        )
    }

    private fun extractKeyPoints(
        segments: List<TranscriptSegmentEntity>
    ): List<String> {
        if (segments.isEmpty()) return emptyList()

        val tokenFreq = HashMap<String, Int>()

        segments.forEach { segment ->
            tokens(segment.text).distinct().forEach { token ->
                tokenFreq[token] = (tokenFreq[token] ?: 0) + 1
            }
        }

        val scored = segments.mapIndexed { index, segment ->
            val terms = tokens(segment.text)
            val contentTerms = terms.filterNot(stopWords::contains)

            val base = if (contentTerms.isEmpty()) {
                0.0
            } else {
                contentTerms
                    .sumOf { (tokenFreq[it] ?: 0).toDouble() }
                    .div(contentTerms.size.toDouble())
            }

            val definitionBoost =
                if (looksLikeDefinition(segment.text)) 3.0 else 0.0

            val lengthBoost =
                if (segment.text.length in 45..240) 1.0 else 0.0

            Triple(
                index,
                base + definitionBoost + lengthBoost,
                normalizeSentence(segment.text)
            )
        }

        val desired = when {
            segments.size < 20 -> 5
            segments.size < 80 -> 8
            else -> 12
        }

        return scored
            .sortedByDescending { it.second }
            .distinctBy { canonical(it.third) }
            .take(desired)
            .sortedBy { it.first }
            .map { it.third }
            .filter { it.length >= 20 }
    }

    private fun buildChapters(
        segments: List<TranscriptSegmentEntity>
    ): List<DraftChapter> {
        if (segments.isEmpty()) return emptyList()

        val totalDuration =
            max(segments.last().endMs, 1L)

        val targetChapters = when {
            totalDuration < 15 * 60_000L -> 3
            totalDuration < 40 * 60_000L -> 5
            totalDuration < 90 * 60_000L -> 7
            else -> 9
        }.coerceAtMost(segments.size)

        val bucketDuration =
            max(totalDuration / targetChapters, 1L)

        val buckets = MutableList(targetChapters) {
            mutableListOf<TranscriptSegmentEntity>()
        }

        segments.forEach { segment ->
            val index = (segment.startMs / bucketDuration)
                .toInt()
                .coerceIn(0, targetChapters - 1)

            buckets[index] += segment
        }

        return buckets
            .filter { it.isNotEmpty() }
            .mapIndexed { chapterIndex, bucket ->
                val keywords = topKeywords(bucket, 3)
                val title = when {
                    keywords.isNotEmpty() ->
                        keywords.joinToString(" • ") {
                            it.replaceFirstChar { char ->
                                char.titlecase(Locale.getDefault())
                            }
                        }
                    else -> "Chapter ${chapterIndex + 1}"
                }

                val summary = extractKeyPoints(bucket)
                    .take(2)
                    .joinToString(" ")

                DraftChapter(
                    title = title.take(80),
                    startMs = bucket.first().startMs,
                    summary = summary.ifBlank {
                        normalizeSentence(bucket.first().text)
                    }.take(360)
                )
            }
    }

    private fun buildFlashcards(
        segments: List<TranscriptSegmentEntity>,
        keyPoints: List<String>
    ): List<DraftFlashcard> {
        val cards = mutableListOf<DraftFlashcard>()

        segments.forEach { segment ->
            val text = normalizeSentence(segment.text)

            definitionPatterns.forEach { pattern ->
                val match = pattern.find(text)
                    ?: return@forEach

                val term = match.groupValues[1]
                    .trim()
                    .trim(',', ':', '-', ' ')

                val definition = match.groupValues[2]
                    .trim()
                    .trimEnd('.', ' ')

                if (
                    term.length in 3..50 &&
                    definition.length in 12..240
                ) {
                    cards += DraftFlashcard(
                        front = "What is $term?",
                        back = definition
                    )
                }
            }
        }

        if (cards.size < 6) {
            keyPoints.forEach { point ->
                val terms = topKeywordsFromText(point, 1)

                if (terms.isNotEmpty() && point.length >= 24) {
                    val term = terms.first()
                    cards += DraftFlashcard(
                        front = "Explain: ${term.replaceFirstChar { it.titlecase() }}",
                        back = point
                    )
                }
            }
        }

        return cards
            .distinctBy {
                canonical(it.front + "|" + it.back)
            }
            .take(20)
    }

    private fun buildQuiz(
        cards: List<DraftFlashcard>
    ): List<DraftQuizQuestion> {
        if (cards.size < 2) return emptyList()

        val answers = cards
            .map { it.back }
            .distinct()

        return cards
            .take(10)
            .mapIndexedNotNull { index, card ->
                val distractors = answers
                    .filter { it != card.back }
                    .let { list ->
                        if (list.size >= 3) {
                            // deterministic rotation, not random fake content
                            (0 until list.size)
                                .map { offset ->
                                    list[(index + offset) % list.size]
                                }
                                .distinct()
                                .take(3)
                        } else {
                            list
                        }
                    }

                if (distractors.size < 3) {
                    return@mapIndexedNotNull null
                }

                val options = (
                    distractors + card.back
                ).shuffled(
                    java.util.Random(
                        (card.front.hashCode() * 31L) +
                            card.back.hashCode().toLong()
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

    private fun topKeywords(
        segments: List<TranscriptSegmentEntity>,
        count: Int
    ): List<String> = topKeywordsFromText(
        segments.joinToString(" ") { it.text },
        count
    )

    private fun topKeywordsFromText(
        text: String,
        count: Int
    ): List<String> = tokens(text)
        .filterNot(stopWords::contains)
        .filter { it.length >= 4 }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenByDescending { it.key.length }
        )
        .map { it.key }
        .distinct()
        .take(count)

    private fun looksLikeDefinition(text: String): Boolean {
        val lower = " ${text.lowercase()} "
        return listOf(
            " is ",
            " means ",
            " refers to ",
            " defined as "
        ).any(lower::contains)
    }

    private fun tokens(text: String): List<String> =
        Regex("""[\p{L}\p{N}]{2,}""")
            .findAll(text.lowercase(Locale.ROOT))
            .map { it.value }
            .toList()

    private fun canonical(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()

    private fun normalizeSentence(text: String): String {
        val value = text
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (value.isBlank()) return value

        return value.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}
