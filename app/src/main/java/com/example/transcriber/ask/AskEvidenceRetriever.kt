package com.example.transcriber.ask

import com.example.transcriber.ask.model.AskEvidence
import com.example.transcriber.ask.model.AskScope
import com.example.transcriber.data.database.AskDao
import com.example.transcriber.data.model.AskEvidenceRow

class AskEvidenceRetriever(
    private val dao: AskDao
) {

    suspend fun retrieve(
        question: String,
        scope: AskScope,
        transcriptId: Long?
    ): List<AskEvidence> {

        val fts =
            AskQueryNormalizer
                .ftsQuery(question)
                ?: return emptyList()

        val primary =
            when (scope) {
                AskScope.TRANSCRIPT -> {
                    val id =
                        transcriptId
                            ?: return emptyList()

                    dao.searchTranscriptEvidence(
                        id,
                        fts,
                        10
                    )
                }

                AskScope.LIBRARY ->
                    dao.searchLibraryEvidence(
                        fts,
                        10
                    )
            }

        if (
            primary.isEmpty()
        ) return emptyList()

        val primaryIds =
            primary
                .map {
                    it.segmentId
                }
                .toSet()

        val expanded =
            LinkedHashMap<
                Long,
                AskEvidenceRow
            >()

        primary.forEach { hit ->
            expanded[hit.segmentId] = hit

            dao.nearbySegments(
                transcriptId = hit.transcriptId,
                minStart = (hit.startMs - 30_000L).coerceAtLeast(0L),
                maxStart = hit.endMs + 30_000L,
                limit = 4
            ).forEach { neighbor ->
                expanded.putIfAbsent(
                    neighbor.segmentId,
                    neighbor
                )
            }
        }

        return expanded
            .values
            .sortedWith(
                compareByDescending<AskEvidenceRow> {
                    it.segmentId in primaryIds
                }.thenBy {
                    it.startMs
                }
            )
            .take(16)
            .mapIndexed { index, row ->
                AskEvidence(
                    segmentId = row.segmentId,
                    transcriptId = row.transcriptId,
                    transcriptTitle = row.transcriptTitle,
                    mediaType = row.mediaType,
                    startMs = row.startMs,
                    endMs = row.endMs,
                    text = row.text,
                    rank = index
                )
            }
    }
}
