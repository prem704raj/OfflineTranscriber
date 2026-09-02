package com.example.transcriber

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.media3.common.util.UnstableApi
import com.example.transcriber.billing.BillingRepository
import com.example.transcriber.billing.EntitlementRepository
import com.example.transcriber.billing.EntitlementStore
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.data.repository.KnowledgeRepository
import com.example.transcriber.data.repository.StudyRepository
import com.example.transcriber.data.repository.TranscriptRepository
import com.example.transcriber.diagnostics.DiagnosticsRepository
import com.example.transcriber.media.MediaAvailabilityChecker
import com.example.transcriber.modelmanager.DeviceModelAdvisor
import com.example.transcriber.modelmanager.ModelManager
import com.example.transcriber.privacy.DataDeletionCoordinator
import com.example.transcriber.queue.AppTranscriptionExecutionGateway
import com.example.transcriber.queue.EnqueueTranscriptionUseCase
import com.example.transcriber.queue.TranscriptionQueueManager
import com.example.transcriber.queue.TranscriptionQueueRepository
import com.example.transcriber.queue.VideoQueueInputPreparer
import com.example.transcriber.settings.AppSettingsRepository
import com.example.transcriber.shortcuts.ShortcutCommandRouter
import com.example.transcriber.storage.StorageManager
import com.example.transcriber.transcription.AudioProcessor
import com.example.transcriber.transcription.WhisperEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class TranscriberApplication : Application() {

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var knowledgeRepository: KnowledgeRepository
        private set

    lateinit var studyRepository: StudyRepository
        private set

    lateinit var askRepository: com.example.transcriber.data.repository.AskRepository
        private set

    lateinit var meetingRepository: com.example.transcriber.data.repository.MeetingRepository
        private set

    lateinit var transcriptRepository: TranscriptRepository
        private set

    lateinit var settingsRepository: AppSettingsRepository
        private set

    lateinit var deviceModelAdvisor: DeviceModelAdvisor
        private set

    lateinit var modelManager: ModelManager
        private set

    lateinit var queueRepository: TranscriptionQueueRepository
        private set

    lateinit var queueManager: TranscriptionQueueManager
        private set

    lateinit var storageManager: StorageManager
        private set

    lateinit var entitlementStore: EntitlementStore
        private set

    lateinit var billingRepository: BillingRepository
        private set

    lateinit var entitlementRepository: EntitlementRepository
        private set

    lateinit var enqueueTranscriptionUseCase: EnqueueTranscriptionUseCase
        private set

    lateinit var diagnosticsRepository: DiagnosticsRepository
        private set

    lateinit var dataDeletionCoordinator: DataDeletionCoordinator
        private set

    lateinit var mediaAvailabilityChecker: MediaAvailabilityChecker
        private set

    lateinit var shortcutCommandRouter: ShortcutCommandRouter
        private set

    lateinit var whisperEngine: WhisperEngine
        private set

    lateinit var speakerRepository: com.example.transcriber.speaker.repository.SpeakerDiarizationRepository
        private set

    lateinit var speakerModelManager: com.example.transcriber.speaker.modelmanager.SpeakerModelManager
        private set

    lateinit var speakerCoordinator: com.example.transcriber.speaker.SpeakerDiarizationCoordinator
        private set

    lateinit var exportSnapshotProvider: com.example.transcriber.export.source.RoomExportSnapshotProvider
        private set

    lateinit var exportSnapshotMetadataProvider: com.example.transcriber.export.source.RoomExportSnapshotMetadataProvider
        private set

    lateinit var exportDocumentAssembler: com.example.transcriber.export.document.ExportDocumentAssembler
        private set

    lateinit var exportCacheManager: com.example.transcriber.export.files.ExportCacheManager
        private set

    lateinit var exportCoordinator: com.example.transcriber.export.ExportCoordinator
        private set

    lateinit var captionProjectProvider: com.example.transcriber.caption.source.CaptionProjectProvider
        private set

    lateinit var captionExportJobStore: com.example.transcriber.caption.export.background.CaptionExportJobStore
        private set

    lateinit var captionExportWorkspace: com.example.transcriber.caption.export.background.CaptionExportWorkspace
        private set

    lateinit var captionExportCoordinator: com.example.transcriber.caption.export.CaptionExportCoordinator
        private set

    lateinit var splitCaptionUseCase: com.example.transcriber.caption.edit.SplitCaptionUseCase
        private set

    lateinit var safeMergeCaptionUseCase: com.example.transcriber.caption.edit.SafeMergeCaptionUseCase
        private set

    lateinit var restoreWorkspace: com.example.transcriber.backup.restore.RestoreWorkspace
        private set

    lateinit var backupManifestCodec: com.example.transcriber.backup.format.BackupManifestCodec
        private set

    lateinit var backupCrypto: com.example.transcriber.backup.crypto.BackupCrypto
        private set

    lateinit var backupSecretVault: com.example.transcriber.backup.crypto.BackupSecretVault
        private set

    lateinit var backupStructureValidator: com.example.transcriber.backup.restore.BackupStructureValidator
        private set

    lateinit var backupArchiveValidator: com.example.transcriber.backup.restore.BackupArchiveValidator
        private set

    lateinit var backupPayloadMaterializer: com.example.transcriber.backup.restore.BackupPayloadMaterializer
        private set

    lateinit var backupSizeEstimator: com.example.transcriber.backup.estimate.BackupSizeEstimator
        private set

    lateinit var backupArchiveWriter: com.example.transcriber.backup.archive.BackupArchiveWriter
        private set

    lateinit var currentTranscriptFingerprintIndex: com.example.transcriber.backup.restore.CurrentTranscriptFingerprintIndex
        private set

    lateinit var portableSettingsRestorer: com.example.transcriber.backup.settings.PortableSettingsRestorer
        private set

    lateinit var restoreCoordinator: com.example.transcriber.backup.restore.RestoreCoordinator
        private set

    lateinit var backupOperationStore: com.example.transcriber.backup.background.BackupOperationStore
        private set

    lateinit var backupOperationCoordinator: com.example.transcriber.backup.background.BackupOperationCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        com.example.transcriber.release.ReleaseContract.requireSafeReleaseBuild()
        val db = AppDatabase.getInstance(this)

        shortcutCommandRouter = ShortcutCommandRouter()

        knowledgeRepository = KnowledgeRepository(
            searchDao = db.searchDao(),
            bookmarkDao = db.bookmarkDao(),
            collectionDao = db.collectionDao()
        )
        studyRepository = StudyRepository(db)
        askRepository = com.example.transcriber.data.repository.AskRepository(db)
        meetingRepository = com.example.transcriber.data.repository.MeetingRepository(db)

        settingsRepository = AppSettingsRepository(this)
        deviceModelAdvisor = DeviceModelAdvisor(this)
        modelManager = ModelManager(this, settingsRepository)

        entitlementStore = EntitlementStore(this)
        billingRepository = BillingRepository(
            context = this,
            entitlementStore = entitlementStore
        )
        entitlementRepository = EntitlementRepository(billingRepository)
        billingRepository.connect()

        whisperEngine = WhisperEngine(this)
        val audioProcessor = AudioProcessor(this)

        val legacyModelManager = com.example.transcriber.transcription.ModelManager(this)
        transcriptRepository = TranscriptRepository(
            context = this,
            modelManager = legacyModelManager,
            whisperEngine = whisperEngine,
            audioProcessor = audioProcessor,
            transcriptDao = db.transcriptDao()
        )

        queueRepository = TranscriptionQueueRepository(db.transcriptionJobDao())

        val executionGateway = AppTranscriptionExecutionGateway(
            context = this,
            whisperEngine = whisperEngine,
            audioProcessor = audioProcessor,
            transcriptDao = db.transcriptDao()
        )

        val inputPreparer = VideoQueueInputPreparer(this)

        queueManager = TranscriptionQueueManager(
            repository = queueRepository,
            modelManager = modelManager,
            inputPreparer = inputPreparer,
            gateway = executionGateway
        )

        storageManager = StorageManager(
            context = this,
            modelManager = modelManager
        )

        speakerRepository = com.example.transcriber.speaker.repository.SpeakerDiarizationRepository(db.speakerDiarizationDao())
        speakerModelManager = com.example.transcriber.speaker.modelmanager.SpeakerModelManager(this)
        speakerCoordinator = com.example.transcriber.speaker.SpeakerDiarizationCoordinator(
            context = this,
            repository = speakerRepository,
            modelManager = speakerModelManager,
            billingRepository = billingRepository
        )

        exportSnapshotProvider = com.example.transcriber.export.source.RoomExportSnapshotProvider(db)
        exportSnapshotMetadataProvider = com.example.transcriber.export.source.RoomExportSnapshotMetadataProvider(db)
        exportDocumentAssembler = com.example.transcriber.export.document.ExportDocumentAssembler(exportSnapshotProvider)
        exportCacheManager = com.example.transcriber.export.files.ExportCacheManager(this)
        val exportSaver = com.example.transcriber.export.files.ExportSaver(contentResolver)
        val exportShareManager = com.example.transcriber.export.files.ExportShareManager(this, exportCacheManager)
        exportCoordinator = com.example.transcriber.export.ExportCoordinator(
            assembler = exportDocumentAssembler,
            cacheManager = exportCacheManager,
            saver = exportSaver,
            shareManager = exportShareManager
        )

        captionProjectProvider = com.example.transcriber.caption.source.RoomCaptionProjectProvider(
            transcriptDao = db.transcriptDao(),
            speakerDao = db.speakerDiarizationDao()
        )
        captionExportWorkspace = com.example.transcriber.caption.export.background.CaptionExportWorkspace(this)
        captionExportJobStore = com.example.transcriber.caption.export.background.CaptionExportJobStore(this)
        captionExportCoordinator = com.example.transcriber.caption.export.CaptionExportCoordinator(
            context = this,
            transcriptDao = db.transcriptDao(),
            entitlementRepository = entitlementRepository,
            workspace = captionExportWorkspace,
            videoExporter = com.example.transcriber.caption.export.CaptionVideoExporter(this)
        )
        splitCaptionUseCase = com.example.transcriber.caption.edit.SplitCaptionUseCase(db)
        safeMergeCaptionUseCase = com.example.transcriber.caption.edit.SafeMergeCaptionUseCase(db)

        restoreWorkspace = com.example.transcriber.backup.restore.RestoreWorkspace(this)
        backupManifestCodec = com.example.transcriber.backup.format.BackupManifestCodec()
        backupCrypto = com.example.transcriber.backup.crypto.BackupCrypto()
        backupSecretVault = com.example.transcriber.backup.crypto.BackupSecretVault()
        backupStructureValidator = com.example.transcriber.backup.restore.BackupStructureValidator()
        backupArchiveValidator = com.example.transcriber.backup.restore.BackupArchiveValidator(
            manifestCodec = backupManifestCodec,
            structureValidator = backupStructureValidator
        )
        backupPayloadMaterializer = com.example.transcriber.backup.restore.BackupPayloadMaterializer(
            context = this,
            workspace = restoreWorkspace,
            crypto = backupCrypto
        )
        backupSizeEstimator = com.example.transcriber.backup.estimate.BackupSizeEstimator(
            context = this,
            dao = db.backupDao()
        )
        backupArchiveWriter = com.example.transcriber.backup.archive.BackupArchiveWriter(
            context = this,
            database = db,
            settingsRepository = settingsRepository,
            manifestCodec = backupManifestCodec,
            crypto = backupCrypto,
            workspace = restoreWorkspace,
            payloadMaterializer = backupPayloadMaterializer,
            archiveValidator = backupArchiveValidator
        )
        currentTranscriptFingerprintIndex = com.example.transcriber.backup.restore.CurrentTranscriptFingerprintIndex(
            dao = db.backupDao()
        )
        portableSettingsRestorer = com.example.transcriber.backup.settings.PortableSettingsRestorer(
            settingsRepository = settingsRepository
        )
        restoreCoordinator = com.example.transcriber.backup.restore.RestoreCoordinator(
            context = this,
            database = db,
            workspace = restoreWorkspace,
            duplicateIndexBuilder = currentTranscriptFingerprintIndex,
            settingsRestorer = portableSettingsRestorer
        )
        backupOperationStore = com.example.transcriber.backup.background.BackupOperationStore()
        backupOperationCoordinator = com.example.transcriber.backup.background.BackupOperationCoordinator(
            context = this,
            store = backupOperationStore,
            vault = backupSecretVault
        )

        enqueueTranscriptionUseCase = EnqueueTranscriptionUseCase(
            context = this,
            settings = settingsRepository,
            modelManager = modelManager,
            deviceAdvisor = deviceModelAdvisor,
            queueRepository = queueRepository,
            entitlementRepository = entitlementRepository
        )

        diagnosticsRepository = DiagnosticsRepository(
            context = this,
            dao = db.diagnosticsDao(),
            settings = settingsRepository,
            modelManager = modelManager,
            storageManager = storageManager,
            billingRepository = billingRepository
        )

        dataDeletionCoordinator = DataDeletionCoordinator(
            context = this,
            database = db,
            privacyDao = db.privacyDataDao(),
            modelManager = modelManager,
            settings = settingsRepository,
            storageManager = storageManager
        )

        mediaAvailabilityChecker = MediaAvailabilityChecker(this)

        applicationScope.launch {
            modelManager.adoptExistingModels()
            queueManager.recoverInterrupted()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            applicationScope.launch {
                if (!queueManager.isProcessing()) {
                    whisperEngine.releaseIdleContext()
                }
            }
        }
    }
}
