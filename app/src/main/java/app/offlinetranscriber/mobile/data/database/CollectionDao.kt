package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.offlinetranscriber.mobile.data.model.CollectionEntity
import app.offlinetranscriber.mobile.data.model.CollectionSummaryRow
import app.offlinetranscriber.mobile.data.model.LibraryTranscriptRow
import app.offlinetranscriber.mobile.data.model.TranscriptCollectionCrossRef
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query(
        """
        SELECT
            c.id AS id,
            c.name AS name,
            c.createdAt AS createdAt,
            COUNT(x.transcriptId) AS transcriptCount
        FROM collections AS c
        LEFT JOIN transcript_collection_cross_ref AS x
            ON x.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.createdAt DESC
        """
    )
    fun observeCollectionSummaries(): Flow<List<CollectionSummaryRow>>

    @Query(
        """
        SELECT
            t.id AS id,
            t.title AS title,
            COALESCE(t.audioUriString, '') AS audioUri,
            t.sourceUri AS sourceUri,
            t.mediaType AS mediaType,
            t.audioDurationMs AS durationMs,
            t.createdAt AS createdAt,
            COUNT(b.id) AS bookmarkCount
        FROM transcripts AS t
        LEFT JOIN bookmarks AS b
            ON b.transcriptId = t.id
        GROUP BY t.id
        ORDER BY t.createdAt DESC
        """
    )
    fun observeLibraryTranscripts(): Flow<List<LibraryTranscriptRow>>

    @Query(
        """
        SELECT t.*
        FROM transcripts AS t
        INNER JOIN transcript_collection_cross_ref AS x
            ON x.transcriptId = t.id
        WHERE x.collectionId = :collectionId
        ORDER BY t.createdAt DESC
        """
    )
    fun observeTranscriptsInCollection(
        collectionId: Long
    ): Flow<List<TranscriptEntity>>

    @Query(
        "SELECT * FROM collections WHERE id = :collectionId LIMIT 1"
    )
    fun observeCollection(
        collectionId: Long
    ): Flow<CollectionEntity?>

    @Query(
        """
        SELECT collectionId
        FROM transcript_collection_cross_ref
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun getCollectionIdsForTranscript(
        transcriptId: Long
    ): List<Long>

    @Insert
    suspend fun insertCollection(
        collection: CollectionEntity
    ): Long

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(
        collectionId: Long
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembership(
        crossRef: TranscriptCollectionCrossRef
    )

    @Query(
        """
        DELETE FROM transcript_collection_cross_ref
        WHERE transcriptId = :transcriptId
          AND collectionId = :collectionId
        """
    )
    suspend fun removeMembership(
        transcriptId: Long,
        collectionId: Long
    )
}
