package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.offlinetranscriber.mobile.data.model.MeetingActionEntity
import app.offlinetranscriber.mobile.data.model.MeetingCitationRow
import app.offlinetranscriber.mobile.data.model.MeetingDecisionEntity
import app.offlinetranscriber.mobile.data.model.MeetingPackEntity
import app.offlinetranscriber.mobile.data.model.MeetingQuestionEntity
import app.offlinetranscriber.mobile.data.model.MeetingSummaryCitationEntity
import app.offlinetranscriber.mobile.data.model.MeetingTopicEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Query("SELECT * FROM meeting_packs WHERE transcriptId = :transcriptId LIMIT 1")
    fun observePack(transcriptId: Long): Flow<MeetingPackEntity?>

    @Query("SELECT * FROM meeting_packs WHERE transcriptId = :transcriptId LIMIT 1")
    suspend fun getPack(transcriptId: Long): MeetingPackEntity?

    @Insert
    suspend fun insertPack(value: MeetingPackEntity): Long

    @Query("DELETE FROM meeting_packs WHERE transcriptId = :transcriptId")
    suspend fun deletePackForTranscript(transcriptId: Long)

    @Insert
    suspend fun insertSummaryCitations(values: List<MeetingSummaryCitationEntity>)

    @Insert
    suspend fun insertActions(values: List<MeetingActionEntity>)

    @Insert
    suspend fun insertDecisions(values: List<MeetingDecisionEntity>)

    @Insert
    suspend fun insertQuestions(values: List<MeetingQuestionEntity>)

    @Insert
    suspend fun insertTopics(values: List<MeetingTopicEntity>)

    @Query("SELECT * FROM meeting_actions WHERE meetingPackId = :packId ORDER BY startMs ASC")
    suspend fun getActionsOnce(packId: Long): List<MeetingActionEntity>

    @Query("SELECT * FROM meeting_actions WHERE meetingPackId = :packId ORDER BY CASE status WHEN 'OPEN' THEN 0 ELSE 1 END, startMs ASC")
    fun observeActions(packId: Long): Flow<List<MeetingActionEntity>>

    @Query("SELECT * FROM meeting_decisions WHERE meetingPackId = :packId ORDER BY startMs ASC")
    suspend fun getDecisionsOnce(packId: Long): List<MeetingDecisionEntity>

    @Query("SELECT * FROM meeting_decisions WHERE meetingPackId = :packId ORDER BY startMs ASC")
    fun observeDecisions(packId: Long): Flow<List<MeetingDecisionEntity>>

    @Query("SELECT * FROM meeting_questions WHERE meetingPackId = :packId ORDER BY startMs ASC")
    suspend fun getQuestionsOnce(packId: Long): List<MeetingQuestionEntity>

    @Query("SELECT * FROM meeting_questions WHERE meetingPackId = :packId ORDER BY startMs ASC")
    fun observeQuestions(packId: Long): Flow<List<MeetingQuestionEntity>>

    @Query("SELECT * FROM meeting_topics WHERE meetingPackId = :packId ORDER BY startMs ASC")
    suspend fun getTopicsOnce(packId: Long): List<MeetingTopicEntity>

    @Query("SELECT * FROM meeting_topics WHERE meetingPackId = :packId ORDER BY startMs ASC")
    fun observeTopics(packId: Long): Flow<List<MeetingTopicEntity>>

    @Query("SELECT * FROM meeting_summary_citations WHERE meetingPackId = :packId ORDER BY position ASC")
    fun observeSummaryCitations(packId: Long): Flow<List<MeetingSummaryCitationEntity>>

    @Query(
        """
        SELECT
            s.id AS segmentId,
            s.startMs AS startMs,
            s.text AS text
        FROM meeting_summary_citations c
        JOIN transcript_segments s
            ON s.id = c.segmentId
        WHERE c.meetingPackId = :packId
        ORDER BY c.position ASC
        """
    )
    fun observeResolvedSummaryCitations(packId: Long): Flow<List<MeetingCitationRow>>

    @Query("UPDATE meeting_actions SET status = :status, updatedAt = :now WHERE id = :actionId")
    suspend fun updateActionStatus(
        actionId: Long,
        status: String,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE meeting_actions
        SET text = :text,
            assignee = :assignee,
            dueText = :dueText,
            manuallyEdited = 1,
            updatedAt = :now
        WHERE id = :actionId
        """
    )
    suspend fun editAction(
        actionId: Long,
        text: String,
        assignee: String,
        dueText: String,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM transcript_segments WHERE transcriptId = :transcriptId ORDER BY startMs ASC")
    suspend fun transcriptSegments(transcriptId: Long): List<TranscriptSegmentEntity>

    @Query("SELECT COUNT(*) FROM meeting_packs")
    suspend fun meetingPackCount(): Int

    @Query("SELECT COUNT(*) FROM meeting_actions")
    suspend fun meetingActionCount(): Int
}
