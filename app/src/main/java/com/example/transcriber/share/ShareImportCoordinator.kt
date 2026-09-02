package com.example.transcriber.share

import android.content.Context
import android.content.Intent
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.background.BackgroundTranscriptionStarter
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.billing.FeatureAccessPolicy
import com.example.transcriber.queue.TranscriptionJobStage
import com.example.transcriber.queue.TranscriptionSourceType
import kotlinx.coroutines.flow.first

class ShareImportCoordinator(
    private val context: Context
) {

    private val app = context.applicationContext as TranscriberApplication

    private val parser = IncomingShareParser(context)
    private val importer = IncomingMediaImporter(context)

    suspend fun handle(
        intent: Intent
    ): ShareImportResult {
        val parsed = parser.parse(intent)

        if (parsed.isEmpty()) {
            return ShareImportResult(
                enqueuedCount = 0,
                videoProRejected = 0,
                queueLimitRejected = 0,
                unsupportedOrFailed = 1,
                needsModel = false
            )
        }

        val cachedPro = app.entitlementStore.cached.first().isPro
        val entitlement = if (cachedPro) Entitlement.PRO else Entitlement.FREE

        val prefs = app.settingsRepository.settings.first()
        val installedModel = app.modelManager.selectedOrBestInstalled()
        val modelId = installedModel?.id ?: app.deviceModelAdvisor.recommend().id

        var enqueued = 0
        var videoRejected = 0
        var queueRejected = 0
        var failed = 0

        for (item in parsed) {
            if (item.kind == IncomingMediaKind.VIDEO && entitlement != Entitlement.PRO) {
                videoRejected++
                continue
            }

            val unfinished = app.queueRepository.unfinishedCount()

            if (!FeatureAccessPolicy.canEnqueue(entitlement, unfinished)) {
                queueRejected++
                continue
            }

            val imported = runCatching {
                importer.import(item, intent.flags)
            }.getOrElse {
                failed++
                continue
            }

            val type = when (imported.kind) {
                IncomingMediaKind.AUDIO -> TranscriptionSourceType.AUDIO
                IncomingMediaKind.VIDEO -> TranscriptionSourceType.VIDEO
            }

            val raw = imported.usableUri.toString()
            val prepared = if (type == TranscriptionSourceType.VIDEO) {
                null
            } else {
                raw
            }

            app.queueRepository.enqueue(
                inputUri = raw,
                sourceUri = raw,
                preparedInputUri = prepared,
                sourceType = type,
                displayName = imported.displayName,
                modelId = modelId,
                languageCode = prefs.languageCode,
                stage = if (prepared == null) {
                    TranscriptionJobStage.PREPARING
                } else {
                    TranscriptionJobStage.TRANSCRIBING
                }
            )

            enqueued++
        }

        val needsModel = installedModel == null && enqueued > 0

        if (enqueued > 0 && !needsModel) {
            BackgroundTranscriptionStarter.start(context)
        }

        return ShareImportResult(
            enqueuedCount = enqueued,
            videoProRejected = videoRejected,
            queueLimitRejected = queueRejected,
            unsupportedOrFailed = failed,
            needsModel = needsModel
        )
    }
}
