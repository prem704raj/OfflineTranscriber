package com.example.transcriber.data.database

import androidx.room.Dao
import androidx.room.Query
import com.example.transcriber.data.model.SearchResultRow

@Dao
interface SearchDao {

    @Query(
        """
        SELECT
            s.transcriptId AS transcriptId,
            s.id AS segmentId,
            t.title AS title,
            s.startMs AS startMs,
            s.endMs AS endMs,
            s.text AS text,
            t.mediaType AS mediaType,
            CASE WHEN b.id IS NULL THEN 0 ELSE 1 END AS isBookmarked
        FROM transcript_segments_fts AS f
        INNER JOIN transcript_segments AS s
            ON s.id = f.rowid
        INNER JOIN transcripts AS t
            ON t.id = s.transcriptId
        LEFT JOIN bookmarks AS b
            ON b.segmentId = s.id
        WHERE transcript_segments_fts MATCH :ftsQuery
          AND (:mediaType IS NULL OR t.mediaType = :mediaType)
          AND (:bookmarksOnly = 0 OR b.id IS NOT NULL)
        ORDER BY t.createdAt DESC, s.startMs ASC
        LIMIT :limit
        """
    )
    suspend fun search(
        ftsQuery: String,
        mediaType: String?,
        bookmarksOnly: Boolean,
        limit: Int = 150
    ): List<SearchResultRow>
}
