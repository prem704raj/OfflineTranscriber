package com.example.transcriber.queue

import com.example.transcriber.modelmanager.ModelCatalog
import com.example.transcriber.modelmanager.ModelManager
import com.example.transcriber.performance.ProgressThrottler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Phase 10 replacement.
 *
 * Implements throttled progress emissions, single model mutex synchronization,
 * and reliable queue lifecycle.
 */
class TranscriptionQueueManager(
    private val repository: TranscriptionQueueRepository,
    private val modelManager: ModelManager,
    private val inputPreparer: QueueInputPreparer,
    private val gateway: TranscriptionExecutionGateway
) {

    private val drainMutex = Mutex()

    private val userCancelled = ConcurrentHashMap.newKeySet<Long>()

    private val systemRequeue = ConcurrentHashMap.newKeySet<Long>()

    @Volatile
    private var currentJobId: Long? = null

    private val _runtime = MutableStateFlow<QueueRuntimeState>(QueueRuntimeState.Idle)
    val runtime = _runtime.asStateFlow()

    fun isProcessing(): Boolean =
        _runtime.value is QueueRuntimeState.Running

    suspend fun recoverInterrupted() {
        repository.recoverInterrupted()
    }

    suspend fun runUntilIdle() {
        drainMutex.withLock {
            while (true) {
                val job = repository.nextQueued() ?: break
                processOne(job)
            }

            _runtime.value = QueueRuntimeState.Idle
        }
    }

    suspend fun retry(id: Long) {
        userCancelled.remove(id)
        systemRequeue.remove(id)
        repository.retry(id)
    }

    suspend fun cancel(id: Long) {
        userCancelled += id

        if (currentJobId == id) {
            inputPreparer.cancelCurrentPreparation()
            gateway.cancelCurrent()
        }

        repository.cancel(id)
    }

    /**
     * Android FGS timeout/system stop:
     * requeue instead of marking user-cancelled.
     */
    suspend fun requeueCurrentForSystemStop() {
        val id = currentJobId ?: return

        systemRequeue += id
        inputPreparer.cancelCurrentPreparation()
        gateway.cancelCurrent()
        repository.retry(id)
    }

    fun cancelNativeWorkForServiceDestroy() {
        val id = currentJobId ?: return

        systemRequeue += id
        inputPreparer.cancelCurrentPreparation()
        gateway.cancelCurrent()
    }

    private suspend fun processOne(job: TranscriptionJobEntity) {
        currentJobId = job.id
        val progressThrottler = ProgressThrottler(250L)

        try {
            if (job.id in userCancelled) {
                repository.cancel(job.id)
                return
            }

            val requested = ModelCatalog.byId(job.modelId)

            val model = if (
                requested != null && modelManager.isInstalled(requested)
            ) {
                requested
            } else {
                modelManager.selectedOrBestInstalled()
            }

            if (model == null) {
                repository.fail(
                    job.id,
                    "No transcription model is installed."
                )
                return
            }

            repository.markProcessing(job.id)

            val initialStage = runCatching {
                TranscriptionJobStage.valueOf(job.stage)
            }.getOrDefault(
                if (job.preparedInputUri == null) {
                    TranscriptionJobStage.PREPARING
                } else {
                    TranscriptionJobStage.TRANSCRIBING
                }
            )

            publishRuntime(
                jobId = job.id,
                stage = initialStage,
                progress = job.progress.coerceIn(0, 99)
            )

            val prepared = inputPreparer.prepare(
                job = job,
                onProgress = { progress ->
                    if (!isCancelled(job.id) && progressThrottler.shouldEmit(progress)) {
                        repository.updateStageAndProgress(
                            id = job.id,
                            stage = TranscriptionJobStage.PREPARING,
                            progress = progress
                        )

                        publishRuntime(
                            jobId = job.id,
                            stage = TranscriptionJobStage.PREPARING,
                            progress = progress
                        )
                    }
                },
                isCancelled = { isCancelled(job.id) }
            )

            if (isCancelled(job.id)) {
                handleAbort(job.id)
                return
            }

            if (job.preparedInputUri != prepared.audioUri) {
                repository.setPreparedInput(
                    id = job.id,
                    preparedUri = prepared.audioUri,
                    progress = prepared.preparationWeightPercent
                )
            }

            val preparationWeight = prepared.preparationWeightPercent
            progressThrottler.reset()

            publishRuntime(
                jobId = job.id,
                stage = TranscriptionJobStage.TRANSCRIBING,
                progress = preparationWeight
            )

            val transcriptId = gateway.execute(
                request = ExecutionRequest(
                    jobId = job.id,
                    inputUri = prepared.audioUri,
                    sourceUri = job.sourceUri,
                    sourceType = runCatching {
                        TranscriptionSourceType.valueOf(job.sourceType)
                    }.getOrDefault(TranscriptionSourceType.AUDIO),
                    displayName = job.displayName,
                    modelPath = modelManager.modelFile(model).absolutePath,
                    languageCode = job.languageCode
                ),
                onProgress = { whisperPercent ->
                    if (!isCancelled(job.id)) {
                        val mapped = preparationWeight + (
                            whisperPercent.coerceIn(0, 100) *
                                (100 - preparationWeight) / 100
                        )

                        val safe = mapped.coerceIn(preparationWeight, 99)

                        if (progressThrottler.shouldEmit(safe)) {
                            repository.progress(
                                job.id,
                                safe
                            )

                            publishRuntime(
                                jobId = job.id,
                                stage = TranscriptionJobStage.TRANSCRIBING,
                                progress = safe
                            )
                        }
                    }
                },
                isCancelled = { isCancelled(job.id) }
            )

            when {
                job.id in systemRequeue -> {
                    repository.retry(job.id)
                }

                job.id in userCancelled -> {
                    repository.cancel(job.id)
                }

                else -> {
                    repository.complete(
                        job.id,
                        transcriptId
                    )
                }
            }
        } catch (_: CancellationException) {
            handleAbort(job.id)
        } catch (t: Throwable) {
            when {
                job.id in systemRequeue -> repository.retry(job.id)
                job.id in userCancelled -> repository.cancel(job.id)
                else -> repository.fail(
                    job.id,
                    t.message ?: "Transcription failed."
                )
            }
        } finally {
            currentJobId = null
            userCancelled.remove(job.id)
            systemRequeue.remove(job.id)
        }
    }

    private suspend fun handleAbort(id: Long) {
        if (id in systemRequeue) {
            repository.retry(id)
        } else {
            repository.cancel(id)
        }
    }

    private fun isCancelled(id: Long): Boolean =
        id in userCancelled || id in systemRequeue

    private suspend fun publishRuntime(
        jobId: Long,
        stage: TranscriptionJobStage,
        progress: Int
    ) {
        val queued = repository.queuedCount()

        _runtime.value = QueueRuntimeState.Running(
            jobId = jobId,
            stage = stage,
            progress = progress.coerceIn(0, 99),
            queuedAfterCurrent = queued
        )
    }
}
