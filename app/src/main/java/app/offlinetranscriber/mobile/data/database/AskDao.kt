package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.offlinetranscriber.mobile.data.model.AskCitationEntity
import app.offlinetranscriber.mobile.data.model.AskCitationRow
import app.offlinetranscriber.mobile.data.model.AskConversationEntity
import app.offlinetranscriber.mobile.data.model.AskEvidenceRow
import app.offlinetranscriber.mobile.data.model.AskMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AskDao {

    @Insert
    suspend fun insertConversation(
        value: AskConversationEntity
    ): Long

    @Insert
    suspend fun insertMessage(
        value: AskMessageEntity
    ): Long

    @Insert
    suspend fun insertCitations(
        values: List<AskCitationEntity>
    )

    @Query(
        "SELECT * FROM ask_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC"
    )
    fun observeMessages(
        conversationId: Long
    ): Flow<List<AskMessageEntity>>

    @Query(
        """
        SELECT * FROM ask_conversations
        WHERE scope = :scope
          AND (
              (:transcriptId IS NULL AND transcriptId IS NULL)
              OR transcriptId = :transcriptId
          )
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun latestConversation(
        scope: String,
        transcriptId: Long?
    ): AskConversationEntity?

    @Query(
        "SELECT * FROM ask_conversations WHERE id = :id LIMIT 1"
    )
    suspend fun getConversation(
        id: Long
    ): AskConversationEntity?

    @Query(
        "SELECT * FROM ask_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC"
    )
    suspend fun getMessagesOnce(
        conversationId: Long
    ): List<AskMessageEntity>

    @Query(
        """
        SELECT
            c.messageId AS messageId,
            c.segmentId AS segmentId,
            s.transcriptId AS transcriptId,
            t.title AS transcriptTitle,
            t.mediaType AS mediaType,
            s.startMs AS startMs,
            s.text AS text,
            c.position AS position
        FROM ask_citations c
        JOIN transcript_segments s
            ON s.id = c.segmentId
        JOIN transcripts t
            ON t.id = s.transcriptId
        JOIN ask_messages m
            ON m.id = c.messageId
        WHERE m.conversationId = :conversationId
        ORDER BY c.position ASC
        """
    )
    suspend fun resolvedCitationsForConversation(
        conversationId: Long
    ): List<AskCitationRow>

    @Query(
        "UPDATE ask_conversations SET updatedAt = :time WHERE id = :id"
    )
    suspend fun touchConversation(
        id: Long,
        time: Long = System.currentTimeMillis()
    )

    @Query(
        "DELETE FROM ask_conversations WHERE id = :conversationId"
    )
    suspend fun deleteConversation(
        conversationId: Long
    )

    @Query(
        "DELETE FROM ask_conversations"
    )
    suspend fun deleteAllConversations()

    @Query(
        """
        SELECT
            s.id AS segmentId,
            s.transcriptId AS transcriptId,
            t.title AS transcriptTitle,
            t.mediaType AS mediaType,
            s.startMs AS startMs,
            s.endMs AS endMs,
            s.text AS text
        FROM transcript_segments_fts f
        JOIN transcript_segments s
            ON s.id = f.rowid
        JOIN transcripts t
            ON t.id = s.transcriptId
        WHERE transcript_segments_fts MATCH :ftsQuery
          AND s.transcriptId = :transcriptId
        LIMIT :limit
        """
    )
    suspend fun searchTranscriptEvidence(
        transcriptId: Long,
        ftsQuery: String,
        limit: Int = 10
    ): List<AskEvidenceRow>

    @Query(
        """
        SELECT
            s.id AS segmentId,
            s.transcriptId AS transcriptId,
            t.title AS transcriptTitle,
            t.mediaType AS mediaType,
            s.startMs AS startMs,
            s.endMs AS endMs,
            s.text AS text
        FROM transcript_segments_fts f
        JOIN transcript_segments s
            ON s.id = f.rowid
        JOIN transcripts t
            ON t.id = s.transcriptId
        WHERE transcript_segments_fts MATCH :ftsQuery
        LIMIT :limit
        """
    )
    suspend fun searchLibraryEvidence(
        ftsQuery: String,
        limit: Int = 10
    ): List<AskEvidenceRow>

    @Query(
        """
        SELECT
            s.id AS segmentId,
            s.transcriptId AS transcriptId,
            t.title AS transcriptTitle,
            t.mediaType AS mediaType,
            s.startMs AS startMs,
            s.endMs AS endMs,
            s.text AS text
        FROM transcript_segments s
        JOIN transcripts t
            ON t.id = s.transcriptId
        WHERE s.transcriptId = :transcriptId
          AND s.startMs BETWEEN :minStart AND :maxStart
        ORDER BY s.startMs ASC
        LIMIT :limit
        """
    )
    suspend fun nearbySegments(
        transcriptId: Long,
        minStart: Long,
        maxStart: Long,
        limit: Int = 4
    ): List<AskEvidenceRow>

    @Query(
        """
        SELECT
            c.messageId AS messageId,
            c.segmentId AS segmentId,
            s.transcriptId AS transcriptId,
            t.title AS transcriptTitle,
            t.mediaType AS mediaType,
            s.startMs AS startMs,
            s.text AS text,
            c.position AS position
        FROM ask_citations c
        JOIN transcript_segments s
            ON s.id = c.segmentId
        JOIN transcripts t
            ON t.id = s.transcriptId
        WHERE c.messageId = :messageId
        ORDER BY c.position ASC
        """
    )
    suspend fun resolvedCitations(
        messageId: Long
    ): List<AskCitationRow>
}
