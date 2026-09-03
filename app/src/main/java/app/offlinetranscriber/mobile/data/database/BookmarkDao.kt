package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.offlinetranscriber.mobile.data.model.BookmarkEntity
import app.offlinetranscriber.mobile.data.model.BookmarkMomentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query(
        """
        SELECT segmentId
        FROM bookmarks
        WHERE transcriptId = :transcriptId
        """
    )
    fun observeBookmarkedSegmentIds(
        transcriptId: Long
    ): Flow<List<Long>>

    @Query(
        """
        SELECT
            b.id AS bookmarkId,
            b.transcriptId AS transcriptId,
            b.segmentId AS segmentId,
            t.title AS title,
            s.startMs AS startMs,
            s.endMs AS endMs,
            s.text AS text,
            t.mediaType AS mediaType,
            b.createdAt AS createdAt
        FROM bookmarks AS b
        INNER JOIN transcript_segments AS s
            ON s.id = b.segmentId
        INNER JOIN transcripts AS t
            ON t.id = b.transcriptId
        ORDER BY b.createdAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentBookmarks(
        limit: Int = 12
    ): Flow<List<BookmarkMomentRow>>

    @Query(
        "SELECT id FROM bookmarks WHERE segmentId = :segmentId LIMIT 1"
    )
    suspend fun bookmarkIdForSegment(
        segmentId: Long
    ): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(
        bookmark: BookmarkEntity
    ): Long

    @Query("DELETE FROM bookmarks WHERE segmentId = :segmentId")
    suspend fun deleteBySegmentId(
        segmentId: Long
    )

    @Transaction
    suspend fun toggle(
        transcriptId: Long,
        segmentId: Long
    ): Boolean {
        val existing = bookmarkIdForSegment(segmentId)

        return if (existing == null) {
            insert(
                BookmarkEntity(
                    transcriptId = transcriptId,
                    segmentId = segmentId
                )
            )
            true
        } else {
            deleteBySegmentId(segmentId)
            false
        }
    }
}
