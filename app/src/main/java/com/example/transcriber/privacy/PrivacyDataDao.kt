package com.example.transcriber.privacy

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PrivacyDataDao {

    @Query("DELETE FROM transcription_jobs")
    suspend fun deleteAllJobs()

    @Query("DELETE FROM transcripts")
    suspend fun deleteAllTranscripts()

    @Query("DELETE FROM collections")
    suspend fun deleteAllCollections()

    @Query("DELETE FROM ask_conversations")
    suspend fun deleteAllAskConversations()

    @Query("DELETE FROM meeting_packs")
    suspend fun deleteAllMeetingPacks()

    @Query("DELETE FROM speaker_diarization_runs")
    suspend fun deleteAllSpeakerRuns()

    @Query("DELETE FROM speaker_clusters")
    suspend fun deleteAllSpeakerClusters()

    @Transaction
    suspend fun deleteTranscriptionContent() {
        deleteAllJobs()
        deleteAllAskConversations()
        deleteAllMeetingPacks()
        deleteAllSpeakerRuns()
        deleteAllSpeakerClusters()
        deleteAllTranscripts()
        deleteAllCollections()
    }
}
