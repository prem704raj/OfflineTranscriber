package app.offlinetranscriber.mobile

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.media3.common.util.UnstableApi
import app.offlinetranscriber.mobile.billing.BillingRepository
import app.offlinetranscriber.mobile.billing.EntitlementRepository
import app.offlinetranscriber.mobile.billing.EntitlementStore
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.repository.KnowledgeRepository
import app.offlinetranscriber.mobile.data.repository.StudyRepository
import app.offlinetranscriber.mobile.data.repository.TranscriptRepository
import app.offlinetranscriber.mobile.diagnostics.DiagnosticsRepository
import app.offlinetranscriber.mobile.media.MediaAvailabilityChecker
import app.offlinetranscriber.mobile.modelmanager.DeviceModelAdvisor
import app.offlinetranscriber.mobile.modelmanager.ModelManager
import app.offlinetranscriber.mobile.privacy.DataDeletionCoordinator
import app.offlinetranscriber.mobile.queue.AppTranscriptionExecutionGateway
import app.offlinetranscriber.mobile.queue.EnqueueTranscriptionUseCase
import app.offlinetranscriber.mobile.queue.TranscriptionQueueManager
import app.offlinetranscriber.mobile.queue.TranscriptionQueueRepository
import app.offlinetranscriber.mobile.queue.VideoQueueInputPreparer
import app.offlinetranscriber.mobile.settings.AppSettingsRepository
import app.offlinetranscriber.mobile.shortcuts.ShortcutCommandRouter
import app.offlinetranscriber.mobile.storage.StorageManager
import app.offlinetranscriber.mobile.transcription.AudioProcessor
import app.offlinetranscriber.mobile.transcription.WhisperEngine
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

    lateinit var askRepository: app.offlinetranscriber.mobile.data.repository.AskRepository
        private set

    lateinit var meetingRepository: app.offlinetranscriber.mobile.data.repository.MeetingRepository
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

    lateinit var speakerRepository: app.offlinetranscriber.mobile.speaker.repository.SpeakerDiarizationRepository
        private set

    lateinit var speakerModelManager: app.offlinetranscriber.mobile.speaker.modelmanager.SpeakerModelManager
        private set

    lateinit var speakerCoordinator: app.offlinetranscriber.mobile.speaker.SpeakerDiarizationCoordinator
        private set

    lateinit var exportSnapshotProvider: app.offlinetranscriber.mobile.export.source.RoomExportSnapshotProvider
        private set

    lateinit var exportSnapshotMetadataProvider: app.offlinetranscriber.mobile.export.source.RoomExportSnapshotMetadataProvider
        private set

    lateinit var exportDocumentAssembler: app.offlinetranscriber.mobile.export.document.ExportDocumentAssembler
        private set

    lateinit var exportCacheManager: app.offlinetranscriber.mobile.export.files.ExportCacheManager
        private set

    lateinit var exportCoordinator: app.offlinetranscriber.mobile.export.ExportCoordinator
        private set

    lateinit var captionProjectProvider: app.offlinetranscriber.mobile.caption.source.CaptionProjectProvider
        private set

    lateinit var captionExportJobStore: app.offlinetranscriber.mobile.caption.export.background.CaptionExportJobStore
        private set

    lateinit var captionExportWorkspace: app.offlinetranscriber.mobile.caption.export.background.CaptionExportWorkspace
        private set

    lateinit var captionExportCoordinator: app.offlinetranscriber.mobile.caption.export.CaptionExportCoordinator
        private set

    lateinit var splitCaptionUseCase: app.offlinetranscriber.mobile.caption.edit.SplitCaptionUseCase
        private set

    lateinit var safeMergeCaptionUseCase: app.offlinetranscriber.mobile.caption.edit.SafeMergeCaptionUseCase
        private set

    lateinit var restoreWorkspace: app.offlinetranscriber.mobile.backup.restore.RestoreWorkspace
        private set

    lateinit var backupManifestCodec: app.offlinetranscriber.mobile.backup.format.BackupManifestCodec
        private set

    lateinit var backupCrypto: app.offlinetranscriber.mobile.backup.crypto.BackupCrypto
        private set

    lateinit var backupSecretVault: app.offlinetranscriber.mobile.backup.crypto.BackupSecretVault
        private set

    lateinit var backupStructureValidator: app.offlinetranscriber.mobile.backup.restore.BackupStructureValidator
        private set

    lateinit var backupArchiveValidator: app.offlinetranscriber.mobile.backup.restore.BackupArchiveValidator
        private set

    lateinit var backupPayloadMaterializer: app.offlinetranscriber.mobile.backup.restore.BackupPayloadMaterializer
        private set

    lateinit var backupSizeEstimator: app.offlinetranscriber.mobile.backup.estimate.BackupSizeEstimator
        private set

    lateinit var backupArchiveWriter: app.offlinetranscriber.mobile.backup.archive.BackupArchiveWriter
        private set

    lateinit var currentTranscriptFingerprintIndex: app.offlinetranscriber.mobile.backup.restore.CurrentTranscriptFingerprintIndex
        private set

    lateinit var portableSettingsRestorer: app.offlinetranscriber.mobile.backup.settings.PortableSettingsRestorer
        private set

    lateinit var restoreCoordinator: app.offlinetranscriber.mobile.backup.restore.RestoreCoordinator
        private set

    lateinit var backupOperationStore: app.offlinetranscriber.mobile.backup.background.BackupOperationStore
        private set

    lateinit var backupOperationCoordinator: app.offlinetranscriber.mobile.backup.background.BackupOperationCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        app.offlinetranscriber.mobile.release.ReleaseContract.requireSafeReleaseBuild()
        val db = AppDatabase.getInstance(this)

        shortcutCommandRouter = ShortcutCommandRouter()

        knowledgeRepository = KnowledgeRepository(
            searchDao = db.searchDao(),
            bookmarkDao = db.bookmarkDao(),
            collectionDao = db.collectionDao()
        )
        studyRepository = StudyRepository(db)
        askRepository = app.offlinetranscriber.mobile.data.repository.AskRepository(db)
        meetingRepository = app.offlinetranscriber.mobile.data.repository.MeetingRepository(db)

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

        val legacyModelManager = app.offlinetranscriber.mobile.transcription.ModelManager(this)
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
            database = db,
            queueRepository = queueRepository
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

        speakerRepository = app.offlinetranscriber.mobile.speaker.repository.SpeakerDiarizationRepository(db.speakerDiarizationDao())
        speakerModelManager = app.offlinetranscriber.mobile.speaker.modelmanager.SpeakerModelManager(this)
        speakerCoordinator = app.offlinetranscriber.mobile.speaker.SpeakerDiarizationCoordinator(
            context = this,
            repository = speakerRepository,
            modelManager = speakerModelManager,
            billingRepository = billingRepository
        )

        exportSnapshotProvider = app.offlinetranscriber.mobile.export.source.RoomExportSnapshotProvider(db)
        exportSnapshotMetadataProvider = app.offlinetranscriber.mobile.export.source.RoomExportSnapshotMetadataProvider(db)
        exportDocumentAssembler = app.offlinetranscriber.mobile.export.document.ExportDocumentAssembler(exportSnapshotProvider)
        exportCacheManager = app.offlinetranscriber.mobile.export.files.ExportCacheManager(this)
        val exportSaver = app.offlinetranscriber.mobile.export.files.ExportSaver(contentResolver)
        val exportShareManager = app.offlinetranscriber.mobile.export.files.ExportShareManager(this, exportCacheManager)
        exportCoordinator = app.offlinetranscriber.mobile.export.ExportCoordinator(
            assembler = exportDocumentAssembler,
            cacheManager = exportCacheManager,
            saver = exportSaver,
            shareManager = exportShareManager
        )

        captionProjectProvider = app.offlinetranscriber.mobile.caption.source.RoomCaptionProjectProvider(
            transcriptDao = db.transcriptDao(),
            speakerDao = db.speakerDiarizationDao()
        )
        captionExportWorkspace = app.offlinetranscriber.mobile.caption.export.background.CaptionExportWorkspace(this)
        captionExportJobStore = app.offlinetranscriber.mobile.caption.export.background.CaptionExportJobStore(this)
        captionExportCoordinator = app.offlinetranscriber.mobile.caption.export.CaptionExportCoordinator(
            context = this,
            transcriptDao = db.transcriptDao(),
            entitlementRepository = entitlementRepository,
            workspace = captionExportWorkspace,
            videoExporter = app.offlinetranscriber.mobile.caption.export.CaptionVideoExporter(this)
        )
        splitCaptionUseCase = app.offlinetranscriber.mobile.caption.edit.SplitCaptionUseCase(db)
        safeMergeCaptionUseCase = app.offlinetranscriber.mobile.caption.edit.SafeMergeCaptionUseCase(db)

        restoreWorkspace = app.offlinetranscriber.mobile.backup.restore.RestoreWorkspace(this)
        backupManifestCodec = app.offlinetranscriber.mobile.backup.format.BackupManifestCodec()
        backupCrypto = app.offlinetranscriber.mobile.backup.crypto.BackupCrypto()
        backupSecretVault = app.offlinetranscriber.mobile.backup.crypto.BackupSecretVault()
        backupStructureValidator = app.offlinetranscriber.mobile.backup.restore.BackupStructureValidator()
        backupArchiveValidator = app.offlinetranscriber.mobile.backup.restore.BackupArchiveValidator(
            manifestCodec = backupManifestCodec,
            structureValidator = backupStructureValidator
        )
        backupPayloadMaterializer = app.offlinetranscriber.mobile.backup.restore.BackupPayloadMaterializer(
            context = this,
            workspace = restoreWorkspace,
            crypto = backupCrypto
        )
        backupSizeEstimator = app.offlinetranscriber.mobile.backup.estimate.BackupSizeEstimator(
            context = this,
            dao = db.backupDao()
        )
        backupArchiveWriter = app.offlinetranscriber.mobile.backup.archive.BackupArchiveWriter(
            context = this,
            database = db,
            settingsRepository = settingsRepository,
            manifestCodec = backupManifestCodec,
            crypto = backupCrypto,
            workspace = restoreWorkspace,
            payloadMaterializer = backupPayloadMaterializer,
            archiveValidator = backupArchiveValidator
        )
        currentTranscriptFingerprintIndex = app.offlinetranscriber.mobile.backup.restore.CurrentTranscriptFingerprintIndex(
            dao = db.backupDao()
        )
        portableSettingsRestorer = app.offlinetranscriber.mobile.backup.settings.PortableSettingsRestorer(
            settingsRepository = settingsRepository
        )
        restoreCoordinator = app.offlinetranscriber.mobile.backup.restore.RestoreCoordinator(
            context = this,
            database = db,
            workspace = restoreWorkspace,
            duplicateIndexBuilder = currentTranscriptFingerprintIndex,
            settingsRestorer = portableSettingsRestorer
        )
        backupOperationStore = app.offlinetranscriber.mobile.backup.background.BackupOperationStore()
        backupOperationCoordinator = app.offlinetranscriber.mobile.backup.background.BackupOperationCoordinator(
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
