package com.example.transcriber.queue

import android.content.Context
import android.net.Uri
import com.example.transcriber.background.BackgroundTranscriptionStarter
import com.example.transcriber.billing.EntitlementRepository
import com.example.transcriber.billing.FeatureAccessPolicy
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.billing.ProRequiredException
import com.example.transcriber.modelmanager.DeviceModelAdvisor
import com.example.transcriber.modelmanager.ModelCatalog
import com.example.transcriber.modelmanager.ModelManager
import com.example.transcriber.settings.AppSettingsRepository
import kotlinx.coroutines.flow.first

data class EnqueueResult(
    val jobId: Long,
    val needsModel: Boolean
)

class EnqueueTranscriptionUseCase(
    private val context: Context,
    private val settings: AppSettingsRepository,
    private val modelManager: ModelManager,
    private val deviceAdvisor: DeviceModelAdvisor,
    private val queueRepository: TranscriptionQueueRepository,
    private val entitlementRepository: EntitlementRepository
) {
    suspend operator fun invoke(
        inputUri: Uri,
        sourceUri: Uri,
        sourceType: TranscriptionSourceType,
        displayName: String,
        isPrepared: Boolean = (sourceType != TranscriptionSourceType.VIDEO)
    ): EnqueueResult {
        val entitlement = entitlementRepository.entitlement.first()

        // 1. Check Batch Queue Concurrency Limit for Free users
        val unfinishedCount = queueRepository.unfinishedCount()
        if (!FeatureAccessPolicy.canEnqueue(entitlement, unfinishedCount)) {
            throw ProRequiredException(ProFeature.BATCH_QUEUE)
        }

        // 2. Check Video Transcription Entitlement
        if (sourceType == TranscriptionSourceType.VIDEO &&
            !FeatureAccessPolicy.hasAccess(entitlement, ProFeature.VIDEO_TRANSCRIPTION)
        ) {
            throw ProRequiredException(ProFeature.VIDEO_TRANSCRIPTION)
        }

        val prefs = settings.settings.first()
        val installed = modelManager.selectedOrBestInstalled()
        val targetModel = installed ?: deviceAdvisor.recommend()

        // 3. Check Accurate Model Entitlement if model is selected
        if (targetModel.id == ModelCatalog.accurate.id &&
            !FeatureAccessPolicy.hasAccess(entitlement, ProFeature.ACCURATE_MODEL)
        ) {
            throw ProRequiredException(ProFeature.ACCURATE_MODEL)
        }

        val raw = inputUri.toString()
        val prepared = if (isPrepared) raw else null

        val id = queueRepository.enqueue(
            inputUri = raw,
            sourceUri = sourceUri.toString(),
            preparedInputUri = prepared,
            sourceType = sourceType,
            displayName = displayName,
            modelId = targetModel.id,
            languageCode = prefs.languageCode,
            stage = if (prepared == null) {
                TranscriptionJobStage.PREPARING
            } else {
                TranscriptionJobStage.TRANSCRIBING
            }
        )

        val needsModel = installed == null

        if (!needsModel) {
            BackgroundTranscriptionStarter.start(context)
        }

        return EnqueueResult(
            jobId = id,
            needsModel = needsModel
        )
    }
}
