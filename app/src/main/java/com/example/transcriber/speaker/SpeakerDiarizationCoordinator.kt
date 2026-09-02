package com.example.transcriber.speaker

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.transcriber.billing.BillingRepository
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.data.model.SpeakerDiarizationRunEntity
import com.example.transcriber.processing.HeavyAudioMlArbiter
import com.example.transcriber.processing.HeavyMlTaskType
import com.example.transcriber.speaker.alignment.SegmentSpeakerAligner
import com.example.transcriber.speaker.audio.DiarizationWindowPolicy
import com.example.transcriber.speaker.audio.Pcm16WindowReader
import com.example.transcriber.speaker.audio.PreparedAudioResult
import com.example.transcriber.speaker.audio.SpeakerAudioPreparer
import com.example.transcriber.speaker.engine.WindowedSpeakerDiarizer
import com.example.transcriber.speaker.model.SpeakerCountMode
import com.example.transcriber.speaker.model.SpeakerDiarizationStatus
import com.example.transcriber.speaker.modelmanager.SpeakerModelCatalog
import com.example.transcriber.speaker.modelmanager.SpeakerModelManager
import com.example.transcriber.speaker.persistence.SpeakerNamePreserver
import com.example.transcriber.speaker.repository.SpeakerDiarizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class SpeakerDiarizationCoordinator(
    private val context: Context,
    private val repository: SpeakerDiarizationRepository,
    private val modelManager: SpeakerModelManager,
    private val billingRepository: BillingRepository,
    private val audioPreparer: SpeakerAudioPreparer = SpeakerAudioPreparer(context)
) {
    companion object {
        private const val TAG = "SpeakerCoordinator"
        private const val ENGINE_VERSION = "sherpa-onnx-1.13.6"
    }

    suspend fun runDiarization(
        transcriptId: Long,
        audioUri: Uri,
        countMode: SpeakerCountMode = SpeakerCountMode.AUTO,
        requestedSpeakerCount: Int? = null,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Unit> = withContext(Dispatchers.Default) {
        // 1. Pro check
        if (!com.example.transcriber.billing.FeatureAccessPolicy.hasAccess(billingRepository.state.value.entitlement, ProFeature.SPEAKER_INTELLIGENCE)) {
            val err = "Speaker Intelligence requires Offline Transcriber Pro"
            repository.setRunStatus(transcriptId, SpeakerDiarizationStatus.FAILED.name, 0, 0, err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        // 2. Model check
        if (!modelManager.areModelsReady()) {
            val err = "Speaker models are not downloaded"
            repository.setRunStatus(transcriptId, SpeakerDiarizationStatus.FAILED.name, 0, 0, err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        val segFile = modelManager.getSegmentationModelFile()
        val embFile = modelManager.getEmbeddingModelFile()

        var preparedAudio: PreparedAudioResult? = null

        try {
            // Initial DB record
            repository.initOrUpdateRun(
                SpeakerDiarizationRunEntity(
                    transcriptId = transcriptId,
                    status = SpeakerDiarizationStatus.PREPARING_AUDIO.name,
                    progress = 5,
                    speakerCountMode = countMode.name,
                    requestedSpeakerCount = requestedSpeakerCount,
                    engineVersion = ENGINE_VERSION,
                    segmentationModelId = SpeakerModelCatalog.SEGMENTATION_MODEL.id,
                    embeddingModelId = SpeakerModelCatalog.EMBEDDING_MODEL.id,
                    errorMessage = null
                )
            )
            onProgress?.invoke(5)

            // 3. Acquire exclusive ML lease
            HeavyAudioMlArbiter.withLease(HeavyMlTaskType.SPEAKER_DIARIZATION) {
                // Step A: Prepare 16kHz mono audio
                Log.i(TAG, "Preparing audio for transcript $transcriptId from URI: $audioUri")
                val prepResult = audioPreparer.preparePcm16(audioUri) { ratio ->
                    val prog = 5 + (ratio * 15f).toInt()
                    onProgress?.invoke(prog)
                }

                if (prepResult.isFailure) {
                    val ex = prepResult.exceptionOrNull() ?: RuntimeException("Audio preparation failed")
                    throw ex
                }

                val prepared = prepResult.getOrThrow()
                preparedAudio = prepared
                val pcmFile = prepared.pcmFile

                repository.setRunStatus(
                    transcriptId = transcriptId,
                    status = SpeakerDiarizationStatus.DIARIZING.name,
                    progress = 20
                )
                onProgress?.invoke(20)

                // Step B: Run Windowed Diarizer
                val windowConfig = DiarizationWindowPolicy.resolveConfig(context)
                val diarizer = WindowedSpeakerDiarizer(
                    segmentationModelFile = segFile,
                    embeddingModelFile = embFile,
                    requestedSpeakerCount = requestedSpeakerCount,
                    windowConfig = windowConfig
                )

                val reader = Pcm16WindowReader(
                    pcmFile = pcmFile,
                    totalSamples = prepared.totalSamples,
                    sampleRate = prepared.sampleRate
                )

                val globalTurns = reader.use { r ->
                    diarizer.diarize(r) { ratio ->
                        val prog = 20 + (ratio * 65f).toInt()
                        onProgress?.invoke(prog)
                    }
                }

                repository.setRunStatus(
                    transcriptId = transcriptId,
                    status = SpeakerDiarizationStatus.ALIGNING.name,
                    progress = 85
                )
                onProgress?.invoke(85)

                // Step C: Match previous names if user previously renamed speakers
                val oldTurns = repository.observeTurns(transcriptId).first()
                val preservedNames = SpeakerNamePreserver.matchPreviousNames(oldTurns, globalTurns)

                // Step D: Align segments to speakers
                val segments = repository.getSegments(transcriptId)
                val uniqueIndices = globalTurns.map { it.globalSpeakerIndex }.distinct().sorted()

                // Create dummy mapping for aligner before DB cluster insertion
                val clusterIndexMap = uniqueIndices.associateWith { it.toLong() }
                val alignedMatches = SegmentSpeakerAligner.alignSegmentsToSpeakers(
                    segments = segments,
                    turns = globalTurns,
                    speakerClusterMap = clusterIndexMap
                )

                // Step E: Atomically persist to Room DB
                repository.saveCompletedDiarization(
                    transcriptId = transcriptId,
                    turns = globalTurns,
                    preservedNames = preservedNames,
                    alignedMatches = alignedMatches,
                    speakerCountMode = countMode.name,
                    requestedSpeakerCount = requestedSpeakerCount,
                    engineVersion = ENGINE_VERSION,
                    segmentationModelId = SpeakerModelCatalog.SEGMENTATION_MODEL.id,
                    embeddingModelId = SpeakerModelCatalog.EMBEDDING_MODEL.id
                )

                onProgress?.invoke(100)
            }

            Log.i(TAG, "Speaker diarization completed successfully for transcript $transcriptId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Diarization failed for transcript $transcriptId", e)
            repository.setRunStatus(
                transcriptId = transcriptId,
                status = SpeakerDiarizationStatus.FAILED.name,
                progress = 0,
                errorMessage = e.message ?: "Diarization failed"
            )
            Result.failure(e)
        } finally {
            // Clean up temporary audio file
            preparedAudio?.pcmFile?.delete()
        }
    }
}
