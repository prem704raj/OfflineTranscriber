package app.offlinetranscriber.mobile.speaker.repository

import app.offlinetranscriber.mobile.data.database.SpeakerAssignmentRow
import app.offlinetranscriber.mobile.data.database.SpeakerDiarizationDao
import app.offlinetranscriber.mobile.data.database.SpeakerTurnRow
import app.offlinetranscriber.mobile.data.model.SegmentSpeakerAssignmentEntity
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity
import app.offlinetranscriber.mobile.data.model.SpeakerDiarizationRunEntity
import app.offlinetranscriber.mobile.data.model.SpeakerTurnEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.speaker.model.GlobalSpeakerTurn
import app.offlinetranscriber.mobile.speaker.model.SegmentSpeakerMatch
import app.offlinetranscriber.mobile.speaker.persistence.PreservedSpeakerName
import kotlinx.coroutines.flow.Flow

class SpeakerDiarizationRepository(
    private val dao: SpeakerDiarizationDao
) {

    fun observeRun(transcriptId: Long): Flow<SpeakerDiarizationRunEntity?> =
        dao.observeRun(transcriptId)

    suspend fun getRun(transcriptId: Long): SpeakerDiarizationRunEntity? =
        dao.getRun(transcriptId)

    fun observeClusters(transcriptId: Long): Flow<List<SpeakerClusterEntity>> =
        dao.observeClusters(transcriptId)

    suspend fun getClustersOnce(transcriptId: Long): List<SpeakerClusterEntity> =
        dao.getClustersOnce(transcriptId)

    fun observeTurns(transcriptId: Long): Flow<List<SpeakerTurnRow>> =
        dao.observeTurns(transcriptId)

    fun observeAssignments(transcriptId: Long): Flow<List<SpeakerAssignmentRow>> =
        dao.observeAssignments(transcriptId)

    suspend fun getSegments(transcriptId: Long): List<TranscriptSegmentEntity> =
        dao.transcriptSegments(transcriptId)

    suspend fun setRunStatus(
        transcriptId: Long,
        status: String,
        progress: Int,
        detectedSpeakerCount: Int = 0,
        errorMessage: String? = null
    ) {
        dao.updateRun(
            transcriptId = transcriptId,
            status = status,
            progress = progress,
            detectedSpeakerCount = detectedSpeakerCount,
            errorMessage = errorMessage
        )
    }

    suspend fun initOrUpdateRun(run: SpeakerDiarizationRunEntity) {
        dao.upsertRun(run)
    }

    suspend fun renameCluster(clusterId: Long, newName: String) {
        val trimmed = newName.trim()
        dao.renameCluster(
            clusterId = clusterId,
            name = trimmed,
            userNamed = trimmed.isNotEmpty()
        )
    }

    suspend fun mergeClusters(sourceClusterId: Long, targetClusterId: Long) {
        if (sourceClusterId == targetClusterId) return
        dao.moveTurns(sourceClusterId = sourceClusterId, targetClusterId = targetClusterId)
        dao.moveAssignments(sourceClusterId = sourceClusterId, targetClusterId = targetClusterId)
        dao.deleteCluster(sourceClusterId)
    }

    suspend fun deleteRunAndData(transcriptId: Long) {
        dao.clearAssignments(transcriptId)
        dao.clearTurns(transcriptId)
        dao.clearClusters(transcriptId)
        dao.deleteRun(transcriptId)
    }

    suspend fun saveCompletedDiarization(
        transcriptId: Long,
        turns: List<GlobalSpeakerTurn>,
        preservedNames: Map<Int, PreservedSpeakerName>,
        alignedMatches: List<SegmentSpeakerMatch>,
        speakerCountMode: String,
        requestedSpeakerCount: Int?,
        engineVersion: String,
        segmentationModelId: String,
        embeddingModelId: String
    ) {
        // 1. Clear previous speaker data for this transcript
        dao.clearAssignments(transcriptId)
        dao.clearTurns(transcriptId)
        dao.clearClusters(transcriptId)

        // 2. Identify unique global speaker indices
        val uniqueIndices = turns.map { it.globalSpeakerIndex }.distinct().sorted()
        val detectedCount = uniqueIndices.size

        // 3. Insert clusters
        val clusterEntities = uniqueIndices.map { index ->
            val preserved = preservedNames[index]
            SpeakerClusterEntity(
                transcriptId = transcriptId,
                speakerIndex = index,
                customName = preserved?.customName ?: "Speaker ${index + 1}",
                userNamed = preserved?.userNamed ?: false
            )
        }
        val insertedClusterIds = dao.insertClusters(clusterEntities)
        val indexToClusterIdMap = uniqueIndices.zip(insertedClusterIds).toMap()

        // 4. Insert turns
        val turnEntities = turns.mapNotNull { turn ->
            val clusterId = indexToClusterIdMap[turn.globalSpeakerIndex] ?: return@mapNotNull null
            SpeakerTurnEntity(
                transcriptId = transcriptId,
                speakerClusterId = clusterId,
                startMs = turn.startMs,
                endMs = turn.endMs
            )
        }
        dao.insertTurns(turnEntities)

        // 5. Insert assignments
        val assignmentEntities = alignedMatches.mapNotNull { match ->
            val clusterId = match.speakerClusterId ?: return@mapNotNull null
            SegmentSpeakerAssignmentEntity(
                segmentId = match.segmentId,
                speakerClusterId = clusterId,
                overlapRatio = match.overlapRatio
            )
        }
        dao.insertAssignments(assignmentEntities)

        // 6. Update run record
        dao.upsertRun(
            SpeakerDiarizationRunEntity(
                transcriptId = transcriptId,
                status = "COMPLETED",
                progress = 100,
                speakerCountMode = speakerCountMode,
                requestedSpeakerCount = requestedSpeakerCount,
                detectedSpeakerCount = detectedCount,
                engineVersion = engineVersion,
                segmentationModelId = segmentationModelId,
                embeddingModelId = embeddingModelId,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
