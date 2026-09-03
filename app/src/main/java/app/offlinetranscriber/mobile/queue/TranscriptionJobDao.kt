package app.offlinetranscriber.mobile.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionJobDao {

    @Query(
        """
        SELECT * FROM transcription_jobs
        ORDER BY
            CASE status
                WHEN 'PROCESSING' THEN 0
                WHEN 'QUEUED' THEN 1
                WHEN 'FAILED' THEN 2
                ELSE 3
            END,
            createdAt ASC
        """
    )
    fun observeAll(): Flow<List<TranscriptionJobEntity>>

    @Query(
        """
        SELECT * FROM transcription_jobs
        WHERE status = 'QUEUED'
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun nextQueued(): TranscriptionJobEntity?

    @Query(
        """
        SELECT * FROM transcription_jobs
        WHERE status = 'PROCESSING'
        ORDER BY updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun currentProcessing(): TranscriptionJobEntity?

    @Query(
        """
        SELECT * FROM transcription_jobs
        WHERE id = :id
        """
    )
    suspend fun getById(id: Long): TranscriptionJobEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(job: TranscriptionJobEntity): Long

    @Query(
        """
        UPDATE transcription_jobs
        SET status = :status,
            progress = :progress,
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateState(
        id: Long,
        status: String,
        progress: Int,
        errorMessage: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET preparedInputUri = :preparedUri,
            stage = 'TRANSCRIBING',
            progress = :progress,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setPreparedInput(
        id: Long,
        preparedUri: String,
        progress: Int = 25,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET stage = :stage,
            progress = :progress,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStageAndProgress(
        id: Long,
        stage: String,
        progress: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET status = 'COMPLETED',
            progress = 100,
            resultTranscriptId = :transcriptId,
            errorMessage = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun complete(
        id: Long,
        transcriptId: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET status = 'QUEUED',
            progress = 0,
            errorMessage = NULL,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun retry(
        id: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET status = 'QUEUED',
            progress = 0,
            errorMessage = 'Recovered after app restart',
            updatedAt = :updatedAt
        WHERE status = 'PROCESSING'
        """
    )
    suspend fun recoverInterrupted(
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        DELETE FROM transcription_jobs
        WHERE status IN ('COMPLETED', 'CANCELLED')
        """
    )
    suspend fun clearFinishedHistory()

    @Query(
        """
        SELECT COUNT(*) FROM transcription_jobs
        WHERE status IN ('QUEUED', 'PROCESSING')
        """
    )
    suspend fun unfinishedCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM transcription_jobs
        WHERE status = 'QUEUED'
        """
    )
    suspend fun queuedCount(): Int

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transcription_jobs
            WHERE status IN ('QUEUED', 'PROCESSING')
        )
        """
    )
    suspend fun hasUnfinished(): Boolean

    @Query(
        """
        UPDATE transcription_jobs
        SET preparedPcmPath = :path,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setPreparedPcmPath(
        id: Long,
        path: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET checkpointSample = :nextSample,
            partialSegmentsJson = :segmentsJson,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun saveCheckpoint(
        id: Long,
        nextSample: Long,
        segmentsJson: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE transcription_jobs
        SET preparedPcmPath = NULL,
            checkpointSample = 0,
            partialSegmentsJson = '[]',
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun clearCheckpoint(
        id: Long,
        updatedAt: Long = System.currentTimeMillis()
    )
}
