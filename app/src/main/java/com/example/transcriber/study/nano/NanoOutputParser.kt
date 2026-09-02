package com.example.transcriber.study.nano

import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.model.DraftChapter
import com.example.transcriber.study.model.DraftFlashcard

data class ParsedNanoChunk(
    val keyPoints: List<String>,
    val chapters: List<DraftChapter>,
    val flashcards: List<DraftFlashcard>
)

object NanoOutputParser {

    /*
    Strict line protocol expected from model:

    KP|text
    CH|timestampMs|title|summary
    FC|question|answer
    */

    fun parse(
        raw: String,
        realSegments: List<TranscriptSegmentEntity>
    ): ParsedNanoChunk {
        val keyPoints = mutableListOf<String>()
        val chapters = mutableListOf<DraftChapter>()
        val flashcards = mutableListOf<DraftFlashcard>()

        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.startsWith("KP|") -> {
                        val value = sanitize(
                            line.substringAfter("KP|")
                        )

                        if (value.length in 15..360) {
                            keyPoints += value
                        }
                    }

                    line.startsWith("CH|") -> {
                        val parts = line.split(
                            "|",
                            limit = 4
                        )

                        if (parts.size == 4) {
                            val requested =
                                parts[1].trim().toLongOrNull()

                            val title = sanitize(parts[2])
                            val summary = sanitize(parts[3])

                            if (
                                requested != null &&
                                title.length in 3..100 &&
                                summary.length in 12..400
                            ) {
                                chapters += DraftChapter(
                                    title = title,
                                    startMs =
                                        nearestRealTimestamp(
                                            requested,
                                            realSegments
                                        ),
                                    summary = summary
                                )
                            }
                        }
                    }

                    line.startsWith("FC|") -> {
                        val parts = line.split(
                            "|",
                            limit = 3
                        )

                        if (parts.size == 3) {
                            val front = sanitize(parts[1])
                            val back = sanitize(parts[2])

                            if (
                                front.length in 5..180 &&
                                back.length in 5..360
                            ) {
                                flashcards += DraftFlashcard(
                                    front = front,
                                    back = back
                                )
                            }
                        }
                    }
                }
            }

        return ParsedNanoChunk(
            keyPoints = keyPoints.distinct().take(12),
            chapters = chapters
                .distinctBy { it.startMs }
                .take(10),
            flashcards = flashcards
                .distinctBy {
                    canonical(it.front + it.back)
                }
                .take(20)
        )
    }

    private fun nearestRealTimestamp(
        requested: Long,
        segments: List<TranscriptSegmentEntity>
    ): Long {
        return segments.minByOrNull {
            kotlin.math.abs(it.startMs - requested)
        }?.startMs ?: 0L
    }

    private fun sanitize(value: String): String =
        value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('|')

    private fun canonical(value: String): String =
        value.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]+"""), "")
}
