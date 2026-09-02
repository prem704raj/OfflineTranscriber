package com.example.transcriber.diagnostics

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DiagnosticsDao {

    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun transcriptCount(): Int

    @Query("SELECT COUNT(*) FROM transcript_segments")
    suspend fun segmentCount(): Int

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun bookmarkCount(): Int

    @Query("SELECT COUNT(*) FROM collections")
    suspend fun collectionCount(): Int

    @Query("SELECT COUNT(*) FROM study_packs")
    suspend fun studyPackCount(): Int

    @Query("SELECT COUNT(*) FROM ask_conversations")
    suspend fun askConversationCount(): Int

    @Query("SELECT COUNT(*) FROM ask_messages")
    suspend fun askMessageCount(): Int

    @Query("SELECT COUNT(*) FROM meeting_packs")
    suspend fun meetingPackCount(): Int

    @Query("SELECT COUNT(*) FROM meeting_actions")
    suspend fun meetingActionCount(): Int

    @Query("SELECT COUNT(*) FROM speaker_clusters")
    suspend fun speakerClusterCount(): Int

    @Query("SELECT COUNT(*) FROM speaker_turns")
    suspend fun speakerTurnCount(): Int

    @Query(
        """
        SELECT COUNT(DISTINCT transcriptId) FROM speaker_diarization_runs
        WHERE status = 'COMPLETED'
        """
    )
    suspend fun diarizedTranscriptCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM transcription_jobs
        WHERE status = :status
        """
    )
    suspend fun queueCount(
        status: String
    ): Int
}
