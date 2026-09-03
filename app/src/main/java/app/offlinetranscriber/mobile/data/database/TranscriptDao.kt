package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptDao {

    @Query("SELECT * FROM transcripts ORDER BY createdAt DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    @Query("SELECT * FROM transcripts WHERE id = :id")
    suspend fun getTranscriptById(id: Long): TranscriptEntity?

    @Query("SELECT * FROM transcripts WHERE id = :id")
    fun observeTranscript(id: Long): Flow<TranscriptEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity): Long

    @Update
    suspend fun updateTranscript(transcript: TranscriptEntity)

    @Query("UPDATE transcripts SET segmentsJson = :segmentsJson, fullText = :fullText WHERE id = :id")
    suspend fun updateSegments(id: Long, segmentsJson: String, fullText: String)

    @Query("UPDATE transcripts SET title = :newTitle WHERE id = :id")
    suspend fun updateTitle(id: Long, newTitle: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity>): List<Long>

    @Query("SELECT * FROM transcript_segments WHERE transcriptId = :transcriptId ORDER BY startMs ASC")
    fun observeSegmentsByTranscriptId(transcriptId: Long): Flow<List<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity>>

    @Query("SELECT * FROM transcript_segments WHERE transcriptId = :transcriptId ORDER BY startMs ASC")
    suspend fun getSegmentsByTranscriptId(transcriptId: Long): List<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity>

    @Query("SELECT * FROM transcript_segments WHERE transcriptId = :transcriptId ORDER BY startMs ASC")
    suspend fun getSegmentsOnce(transcriptId: Long): List<app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity>

    @Query("UPDATE transcript_segments SET text = :text, startMs = :startMs, endMs = :endMs WHERE id = :segmentId")
    suspend fun updateSegmentEntity(segmentId: Long, text: String, startMs: Long, endMs: Long)

    @Query("DELETE FROM transcript_segments WHERE transcriptId = :transcriptId")
    suspend fun deleteSegmentsByTranscriptId(transcriptId: Long)

    @Delete
    suspend fun deleteTranscript(transcript: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcripts")
    suspend fun clearAll()
}
