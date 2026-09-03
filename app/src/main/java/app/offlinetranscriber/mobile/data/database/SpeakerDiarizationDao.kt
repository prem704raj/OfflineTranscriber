package app.offlinetranscriber.mobile.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.offlinetranscriber.mobile.data.model.SegmentSpeakerAssignmentEntity
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity
import app.offlinetranscriber.mobile.data.model.SpeakerDiarizationRunEntity
import app.offlinetranscriber.mobile.data.model.SpeakerTurnEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

data class SpeakerAssignmentRow(
    val segmentId: Long,
    val speakerClusterId: Long,
    val speakerIndex: Int,
    val customName: String,
    val userNamed: Boolean,
    val overlapRatio: Float
)

data class SpeakerTurnRow(
    val id: Long,
    val speakerClusterId: Long,
    val speakerIndex: Int,
    val customName: String,
    val userNamed: Boolean,
    val startMs: Long,
    val endMs: Long
)

@Dao
interface SpeakerDiarizationDao {

    @Query(
        """
        SELECT * FROM speaker_diarization_runs
        WHERE transcriptId = :transcriptId
        LIMIT 1
        """
    )
    fun observeRun(
        transcriptId: Long
    ): Flow<SpeakerDiarizationRunEntity?>

    @Query(
        """
        SELECT * FROM speaker_diarization_runs
        WHERE transcriptId = :transcriptId
        LIMIT 1
        """
    )
    suspend fun getRun(
        transcriptId: Long
    ): SpeakerDiarizationRunEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun upsertRun(
        value: SpeakerDiarizationRunEntity
    ): Long

    @Query(
        """
        UPDATE speaker_diarization_runs
        SET status = :status,
            progress = :progress,
            detectedSpeakerCount = :detectedSpeakerCount,
            errorMessage = :errorMessage,
            updatedAt = :now
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun updateRun(
        transcriptId: Long,
        status: String,
        progress: Int,
        detectedSpeakerCount: Int,
        errorMessage: String?,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE speaker_diarization_runs
        SET status = :status,
            progress = :progress,
            speakerCountMode = :speakerCountMode,
            requestedSpeakerCount = :requestedSpeakerCount,
            engineVersion = :engineVersion,
            segmentationModelId = :segmentationModelId,
            embeddingModelId = :embeddingModelId,
            errorMessage = :errorMessage,
            updatedAt = :now
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun updateRunConfiguration(
        transcriptId: Long,
        status: String,
        progress: Int,
        speakerCountMode: String,
        requestedSpeakerCount: Int?,
        engineVersion: String,
        segmentationModelId: String,
        embeddingModelId: String,
        errorMessage: String?,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        DELETE FROM speaker_diarization_runs
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun deleteRun(
        transcriptId: Long
    )

    @Query(
        """
        DELETE FROM segment_speaker_assignments
        WHERE segmentId IN (
            SELECT id
            FROM transcript_segments
            WHERE transcriptId = :transcriptId
        )
        """
    )
    suspend fun clearAssignments(
        transcriptId: Long
    )

    @Query(
        """
        DELETE FROM speaker_turns
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun clearTurns(
        transcriptId: Long
    )

    @Query(
        """
        DELETE FROM speaker_clusters
        WHERE transcriptId = :transcriptId
        """
    )
    suspend fun clearClusters(
        transcriptId: Long
    )

    @Insert
    suspend fun insertClusters(
        values: List<SpeakerClusterEntity>
    ): List<Long>

    @Insert
    suspend fun insertTurns(
        values: List<SpeakerTurnEntity>
    )

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertAssignments(
        values: List<SegmentSpeakerAssignmentEntity>
    )

    @Query(
        """
        SELECT * FROM speaker_clusters
        WHERE transcriptId = :transcriptId
        ORDER BY speakerIndex ASC
        """
    )
    fun observeClusters(
        transcriptId: Long
    ): Flow<List<SpeakerClusterEntity>>

    @Query(
        """
        SELECT * FROM speaker_clusters
        WHERE transcriptId = :transcriptId
        ORDER BY speakerIndex ASC
        """
    )
    suspend fun getClustersOnce(
        transcriptId: Long
    ): List<SpeakerClusterEntity>

    @Query(
        """
        SELECT
            a.segmentId AS segmentId,
            a.speakerClusterId AS speakerClusterId,
            c.speakerIndex AS speakerIndex,
            c.customName AS customName,
            c.userNamed AS userNamed,
            a.overlapRatio AS overlapRatio
        FROM segment_speaker_assignments a
        JOIN speaker_clusters c
            ON c.id = a.speakerClusterId
        JOIN transcript_segments s
            ON s.id = a.segmentId
        WHERE s.transcriptId = :transcriptId
        """
    )
    fun observeAssignments(
        transcriptId: Long
    ): Flow<List<SpeakerAssignmentRow>>

    @Query(
        """
        SELECT
            a.segmentId AS segmentId,
            a.speakerClusterId AS speakerClusterId,
            c.speakerIndex AS speakerIndex,
            c.customName AS customName,
            c.userNamed AS userNamed,
            a.overlapRatio AS overlapRatio
        FROM segment_speaker_assignments a
        JOIN speaker_clusters c
            ON c.id = a.speakerClusterId
        JOIN transcript_segments s
            ON s.id = a.segmentId
        WHERE s.transcriptId = :transcriptId
        """
    )
    suspend fun getAssignmentsOnce(
        transcriptId: Long
    ): List<SpeakerAssignmentRow>

    @Query(
        """
        SELECT
            t.id AS id,
            t.speakerClusterId AS speakerClusterId,
            c.speakerIndex AS speakerIndex,
            c.customName AS customName,
            c.userNamed AS userNamed,
            t.startMs AS startMs,
            t.endMs AS endMs
        FROM speaker_turns t
        JOIN speaker_clusters c
            ON c.id = t.speakerClusterId
        WHERE t.transcriptId = :transcriptId
        ORDER BY t.startMs ASC
        """
    )
    fun observeTurns(
        transcriptId: Long
    ): Flow<List<SpeakerTurnRow>>

    @Query(
        """
        UPDATE speaker_clusters
        SET customName = :name,
            userNamed = :userNamed,
            updatedAt = :now
        WHERE id = :clusterId
        """
    )
    suspend fun renameCluster(
        clusterId: Long,
        name: String,
        userNamed: Boolean,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE speaker_turns
        SET speakerClusterId = :targetClusterId
        WHERE speakerClusterId = :sourceClusterId
        """
    )
    suspend fun moveTurns(
        sourceClusterId: Long,
        targetClusterId: Long
    )

    @Query(
        """
        UPDATE segment_speaker_assignments
        SET speakerClusterId = :targetClusterId
        WHERE speakerClusterId = :sourceClusterId
        """
    )
    suspend fun moveAssignments(
        sourceClusterId: Long,
        targetClusterId: Long
    )

    @Query(
        """
        DELETE FROM speaker_clusters
        WHERE id = :clusterId
        """
    )
    suspend fun deleteCluster(
        clusterId: Long
    )

    @Query(
        """
        SELECT * FROM transcript_segments
        WHERE transcriptId = :transcriptId
        ORDER BY startMs ASC
        """
    )
    suspend fun transcriptSegments(
        transcriptId: Long
    ): List<TranscriptSegmentEntity>

    @Query(
        """
        SELECT COUNT(*) FROM speaker_clusters
        """
    )
    suspend fun speakerClusterCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM speaker_turns
        """
    )
    suspend fun speakerTurnCount(): Int

    @Query(
        """
        SELECT COUNT(DISTINCT transcriptId) FROM speaker_diarization_runs
        WHERE status = 'COMPLETED'
        """
    )
    suspend fun diarizedTranscriptCount(): Int
}
