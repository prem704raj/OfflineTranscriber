package app.offlinetranscriber.mobile.queue

import kotlinx.coroutines.flow.Flow

class TranscriptionQueueRepository(
    private val dao: TranscriptionJobDao
) {
    fun observeAll(): Flow<List<TranscriptionJobEntity>> =
        dao.observeAll()

    suspend fun enqueue(
        inputUri: String,
        sourceUri: String,
        preparedInputUri: String?,
        sourceType: TranscriptionSourceType,
        displayName: String,
        modelId: String,
        languageCode: String,
        stage: TranscriptionJobStage =
            if (preparedInputUri == null) {
                TranscriptionJobStage.PREPARING
            } else {
                TranscriptionJobStage.TRANSCRIBING
            }
    ): Long = dao.insert(
        TranscriptionJobEntity(
            inputUri = inputUri,
            sourceUri = sourceUri,
            preparedInputUri = preparedInputUri,
            sourceType = sourceType.name,
            displayName = displayName,
            modelId = modelId,
            languageCode = languageCode,
            stage = stage.name
        )
    )

    suspend fun setPreparedInput(
        id: Long,
        preparedUri: String,
        progress: Int = 25
    ) = dao.setPreparedInput(
        id = id,
        preparedUri = preparedUri,
        progress = progress
    )

    suspend fun updateStageAndProgress(
        id: Long,
        stage: TranscriptionJobStage,
        progress: Int
    ) = dao.updateStageAndProgress(
        id = id,
        stage = stage.name,
        progress = progress.coerceIn(0, 99)
    )

    suspend fun nextQueued(): TranscriptionJobEntity? =
        dao.nextQueued()

    suspend fun markProcessing(
        id: Long
    ) = dao.updateState(
        id = id,
        status = TranscriptionJobStatus.PROCESSING.name,
        progress = 0,
        errorMessage = null
    )

    suspend fun progress(
        id: Long,
        value: Int
    ) = dao.updateState(
        id = id,
        status = TranscriptionJobStatus.PROCESSING.name,
        progress = value.coerceIn(0, 99),
        errorMessage = null
    )

    suspend fun complete(
        id: Long,
        transcriptId: Long
    ) = dao.complete(
        id,
        transcriptId
    )

    suspend fun fail(
        id: Long,
        message: String
    ) = dao.updateState(
        id = id,
        status = TranscriptionJobStatus.FAILED.name,
        progress = 0,
        errorMessage = message
    )

    suspend fun cancel(
        id: Long
    ) = dao.updateState(
        id = id,
        status = TranscriptionJobStatus.CANCELLED.name,
        progress = 0,
        errorMessage = null
    )

    suspend fun retry(
        id: Long
    ) = dao.retry(id)

    suspend fun recoverInterrupted() =
        dao.recoverInterrupted()

    suspend fun clearFinishedHistory() =
        dao.clearFinishedHistory()

    suspend fun unfinishedCount(): Int =
        dao.unfinishedCount()

    suspend fun queuedCount(): Int =
        dao.queuedCount()

    suspend fun hasUnfinished(): Boolean =
        dao.hasUnfinished()

    suspend fun getById(id: Long): TranscriptionJobEntity? =
        dao.getById(id)

    suspend fun setPreparedPcmPath(id: Long, path: String) =
        dao.setPreparedPcmPath(id, path)

    suspend fun saveCheckpoint(id: Long, nextSample: Long, segmentsJson: String) =
        dao.saveCheckpoint(id, nextSample, segmentsJson)

    suspend fun clearCheckpoint(id: Long) =
        dao.clearCheckpoint(id)
}
