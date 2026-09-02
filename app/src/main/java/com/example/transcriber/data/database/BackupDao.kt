package com.example.transcriber.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.transcriber.data.model.AskCitationEntity
import com.example.transcriber.data.model.AskConversationEntity
import com.example.transcriber.data.model.AskMessageEntity
import com.example.transcriber.data.model.BookmarkEntity
import com.example.transcriber.data.model.CollectionEntity
import com.example.transcriber.data.model.FlashcardEntity
import com.example.transcriber.data.model.MeetingActionEntity
import com.example.transcriber.data.model.MeetingDecisionEntity
import com.example.transcriber.data.model.MeetingPackEntity
import com.example.transcriber.data.model.MeetingQuestionEntity
import com.example.transcriber.data.model.MeetingSummaryCitationEntity
import com.example.transcriber.data.model.MeetingTopicEntity
import com.example.transcriber.data.model.QuizQuestionEntity
import com.example.transcriber.data.model.SegmentSpeakerAssignmentEntity
import com.example.transcriber.data.model.SpeakerClusterEntity
import com.example.transcriber.data.model.SpeakerDiarizationRunEntity
import com.example.transcriber.data.model.SpeakerTurnEntity
import com.example.transcriber.data.model.StudyChapterEntity
import com.example.transcriber.data.model.StudyKeyPointEntity
import com.example.transcriber.data.model.StudyPackEntity
import com.example.transcriber.data.model.TranscriptCollectionCrossRef
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity

@Dao
interface BackupDao {

    // --- TRANSCRIPTS ---
    @Query("SELECT * FROM transcripts WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupTranscriptsAfter(afterId: Long, limit: Int): List<TranscriptEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTranscript(entity: TranscriptEntity): Long

    // --- TRANSCRIPT SEGMENTS ---
    @Query("SELECT * FROM transcript_segments WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupSegmentsAfter(afterId: Long, limit: Int): List<TranscriptSegmentEntity>

    @Query("SELECT * FROM transcript_segments WHERE transcriptId = :transcriptId ORDER BY startMs ASC, id ASC")
    suspend fun getSegmentsForTranscript(transcriptId: Long): List<TranscriptSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSegment(entity: TranscriptSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSegments(entities: List<TranscriptSegmentEntity>): List<Long>

    // --- BOOKMARKS ---
    @Query("SELECT * FROM bookmarks WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupBookmarksAfter(afterId: Long, limit: Int): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(entity: BookmarkEntity): Long

    // --- COLLECTIONS ---
    @Query("SELECT * FROM collections WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupCollectionsAfter(afterId: Long, limit: Int): List<CollectionEntity>

    @Query("SELECT * FROM collections ORDER BY id ASC")
    suspend fun getAllCollections(): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(entity: CollectionEntity): Long

    // --- COLLECTION MEMBERSHIPS ---
    @Query("SELECT * FROM transcript_collection_cross_ref ORDER BY transcriptId ASC, collectionId ASC LIMIT :limit OFFSET :offset")
    suspend fun backupCollectionMemberships(limit: Int, offset: Int): List<TranscriptCollectionCrossRef>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionMembership(entity: TranscriptCollectionCrossRef)

    // --- STUDY ---
    @Query("SELECT * FROM study_packs WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupStudyPacksAfter(afterId: Long, limit: Int): List<StudyPackEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudyPack(entity: StudyPackEntity): Long

    @Query("SELECT * FROM study_key_points WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupStudyKeyPointsAfter(afterId: Long, limit: Int): List<StudyKeyPointEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudyKeyPoints(entities: List<StudyKeyPointEntity>)

    @Query("SELECT * FROM study_chapters WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupStudyChaptersAfter(afterId: Long, limit: Int): List<StudyChapterEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudyChapters(entities: List<StudyChapterEntity>)

    @Query("SELECT * FROM flashcards WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupFlashcardsAfter(afterId: Long, limit: Int): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFlashcards(entities: List<FlashcardEntity>)

    @Query("SELECT * FROM quiz_questions WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupQuizQuestionsAfter(afterId: Long, limit: Int): List<QuizQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertQuizQuestions(entities: List<QuizQuestionEntity>)

    // --- ASK ---
    @Query("SELECT * FROM ask_conversations WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupAskConversationsAfter(afterId: Long, limit: Int): List<AskConversationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAskConversation(entity: AskConversationEntity): Long

    @Query("SELECT * FROM ask_messages WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupAskMessagesAfter(afterId: Long, limit: Int): List<AskMessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAskMessage(entity: AskMessageEntity): Long

    @Query("SELECT * FROM ask_citations WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupAskCitationsAfter(afterId: Long, limit: Int): List<AskCitationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAskCitations(entities: List<AskCitationEntity>)

    // --- MEETING ---
    @Query("SELECT * FROM meeting_packs WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupMeetingPacksAfter(afterId: Long, limit: Int): List<MeetingPackEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeetingPack(entity: MeetingPackEntity): Long

    @Query("SELECT * FROM meeting_summary_citations ORDER BY meetingPackId ASC, segmentId ASC LIMIT :limit OFFSET :offset")
    suspend fun backupMeetingSummaryCitations(limit: Int, offset: Int): List<MeetingSummaryCitationEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeetingSummaryCitations(entities: List<MeetingSummaryCitationEntity>)

    @Query("SELECT * FROM meeting_actions WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupMeetingActionsAfter(afterId: Long, limit: Int): List<MeetingActionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeetingActions(entities: List<MeetingActionEntity>)

    @Query("SELECT * FROM meeting_decisions WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupMeetingDecisionsAfter(afterId: Long, limit: Int): List<MeetingDecisionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeetingDecisions(entities: List<MeetingDecisionEntity>)

    @Query("SELECT * FROM meeting_questions WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupMeetingQuestionsAfter(afterId: Long, limit: Int): List<MeetingQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeetingQuestions(entities: List<MeetingQuestionEntity>)

    @Query("SELECT * FROM meeting_topics WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupMeetingTopicsAfter(afterId: Long, limit: Int): List<MeetingTopicEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMeetingTopics(entities: List<MeetingTopicEntity>)

    // --- SPEAKER ---
    @Query("SELECT * FROM speaker_clusters WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupSpeakerClustersAfter(afterId: Long, limit: Int): List<SpeakerClusterEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpeakerCluster(entity: SpeakerClusterEntity): Long

    @Query("SELECT * FROM speaker_turns WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupSpeakerTurnsAfter(afterId: Long, limit: Int): List<SpeakerTurnEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSpeakerTurns(entities: List<SpeakerTurnEntity>)

    @Query("SELECT * FROM segment_speaker_assignments ORDER BY segmentId ASC LIMIT :limit OFFSET :offset")
    suspend fun backupSegmentSpeakerAssignments(limit: Int, offset: Int): List<SegmentSpeakerAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegmentSpeakerAssignments(entities: List<SegmentSpeakerAssignmentEntity>)

    @Query("SELECT * FROM speaker_diarization_runs WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun backupSpeakerDiarizationRunsAfter(afterId: Long, limit: Int): List<SpeakerDiarizationRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakerDiarizationRun(entity: SpeakerDiarizationRunEntity): Long

    // --- COUNTS & CLEANUP ---
    @Query("DELETE FROM transcripts")
    suspend fun deleteAllTranscripts()

    @Query("DELETE FROM collections")
    suspend fun deleteAllCollections()

    @Query("DELETE FROM ask_conversations")
    suspend fun deleteAllAskConversations()

    @Query("DELETE FROM transcription_jobs")
    suspend fun deleteAllTranscriptionJobs()
}
