package app.offlinetranscriber.mobile.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import app.offlinetranscriber.mobile.BuildConfig
import app.offlinetranscriber.mobile.billing.BillingRepository
import app.offlinetranscriber.mobile.modelmanager.ModelManager
import app.offlinetranscriber.mobile.queue.TranscriptionJobStatus
import app.offlinetranscriber.mobile.settings.AppSettingsRepository
import app.offlinetranscriber.mobile.settings.LanguageCatalog
import app.offlinetranscriber.mobile.storage.StorageManager
import kotlinx.coroutines.flow.first
import java.io.File

class DiagnosticsRepository(
    private val context: Context,
    private val dao: DiagnosticsDao,
    private val settings: AppSettingsRepository,
    private val modelManager: ModelManager,
    private val storageManager: StorageManager,
    private val billingRepository: BillingRepository
) {

    suspend fun collect(
        nanoStatus: String
    ): AppDiagnostics {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memory)

        val prefs = settings.settings.first()
        val selected = modelManager.selectedOrBestInstalled()
        val storage = storageManager.usage()

        val dbFile = context.getDatabasePath("transcriber_database.db")
        val stat = StatFs(context.filesDir.absolutePath)
        val billing = billingRepository.state.value

        return AppDiagnostics(
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            androidRelease = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            totalRamBytes = memory.totalMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            activeModelLabel = selected?.label ?: "None",
            installedModelLabels = modelManager.installedModels().map { it.label },
            languageLabel = LanguageCatalog.byCode(prefs.languageCode).label,
            databaseBytes = dbFile.takeIf { it.exists() }?.length() ?: 0L,
            transcriptCount = dao.transcriptCount(),
            segmentCount = dao.segmentCount(),
            bookmarkCount = dao.bookmarkCount(),
            collectionCount = dao.collectionCount(),
            studyPackCount = dao.studyPackCount(),
            askConversationCount = dao.askConversationCount(),
            askMessageCount = dao.askMessageCount(),
            meetingPackCount = dao.meetingPackCount(),
            meetingActionCount = dao.meetingActionCount(),
            speakerClusterCount = dao.speakerClusterCount(),
            speakerTurnCount = dao.speakerTurnCount(),
            diarizedTranscriptCount = dao.diarizedTranscriptCount(),
            queue = QueueDiagnostics(
                queued = dao.queueCount(TranscriptionJobStatus.QUEUED.name),
                processing = dao.queueCount(TranscriptionJobStatus.PROCESSING.name),
                completed = dao.queueCount(TranscriptionJobStatus.COMPLETED.name),
                failed = dao.queueCount(TranscriptionJobStatus.FAILED.name),
                cancelled = dao.queueCount(TranscriptionJobStatus.CANCELLED.name)
            ),
            recordingsBytes = storage.recordingsBytes,
            extractedAudioBytes = storage.extractedAudioBytes,
            temporaryBytes = storage.temporaryBytes,
            modelsBytes = storage.modelsBytes,
            freeInternalBytes = stat.availableBytes,
            entitlement = billing.entitlement.name,
            nanoStatus = nanoStatus
        )
    }
}
