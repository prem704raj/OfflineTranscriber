package com.example.transcriber.backup.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.backup.BackupOperationStatus
import com.example.transcriber.backup.BackupOperationType
import com.example.transcriber.backup.RestorePreview
import com.example.transcriber.backup.crypto.InvalidBackupPasswordException
import com.example.transcriber.processing.HeavyProcessingArbiter
import com.example.transcriber.processing.HeavyProcessingOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupRestoreForegroundService : Service() {

    companion object {
        const val ACTION_CREATE = "com.example.transcriber.backup.ACTION_CREATE"
        const val ACTION_INSPECT = "com.example.transcriber.backup.ACTION_INSPECT"
        const val ACTION_RESTORE = "com.example.transcriber.backup.ACTION_RESTORE"
        const val ACTION_CANCEL = "com.example.transcriber.backup.ACTION_CANCEL"

        const val EXTRA_OPERATION_ID = "extra_operation_id"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: TranscriberApplication
    private lateinit var notifications: BackupNotificationFactory

    private var activeJob: Job? = null
    private var activeOperationId: String? = null

    override fun onCreate() {
        super.onCreate()
        app = application as TranscriberApplication
        notifications = BackupNotificationFactory(this)
        notifications.ensureChannel()

        startForegroundNow("Initializing", 0, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val opId = intent?.getStringExtra(EXTRA_OPERATION_ID)

        when (intent?.action) {
            ACTION_CANCEL -> {
                if (opId != null && opId == activeOperationId) {
                    activeJob?.cancel()
                }
            }
            ACTION_CREATE -> {
                if (opId != null) {
                    val request = app.backupOperationCoordinator.takeCreateRequest(opId)
                    if (request != null) {
                        runCreateBackup(request, startId)
                    }
                }
            }
            ACTION_INSPECT -> {
                if (opId != null) {
                    val request = app.backupOperationCoordinator.takeInspectRequest(opId)
                    if (request != null) {
                        runInspectBackup(request, startId)
                    }
                }
            }
            ACTION_RESTORE -> {
                if (opId != null) {
                    val request = app.backupOperationCoordinator.takeRestoreRequest(opId)
                    if (request != null) {
                        runRestoreBackup(request, startId)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun runCreateBackup(request: BackupCreateRequest, startId: Int) {
        activeOperationId = request.operationId
        val secret = app.backupSecretVault.take(request.operationId)

        activeJob = scope.launch {
            try {
                HeavyProcessingArbiter.withLease(HeavyProcessingOwner.BACKUP_RESTORE) {
                    app.backupOperationStore.updateState(
                        request.operationId,
                        BackupOperationType.CREATE,
                        BackupOperationStatus.RUNNING,
                        0,
                        "Starting backup creation..."
                    )
                    startForegroundNow("Creating backup", 0, request.operationId)

                    val result = app.backupArchiveWriter.write(
                        destination = request.destination,
                        options = request.options,
                        password = secret,
                        onProgress = { progress ->
                            app.backupOperationStore.updateState(
                                request.operationId,
                                BackupOperationType.CREATE,
                                BackupOperationStatus.RUNNING,
                                progress.percent,
                                progress.stage
                            )
                            updateNotification("Creating backup", progress.percent, progress.stage, request.operationId)
                        },
                        cancelled = { activeJob?.isCancelled == true }
                    )

                    app.backupOperationStore.updateState(
                        request.operationId,
                        BackupOperationType.CREATE,
                        BackupOperationStatus.COMPLETED,
                        100,
                        "Backup created successfully"
                    )
                    notifications.notifyCompleted("Backup Created", "Backup successfully saved to storage.")
                }
            } catch (e: CancellationException) {
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.CREATE,
                    BackupOperationStatus.CANCELLED,
                    0,
                    "Backup cancelled"
                )
            } catch (e: Throwable) {
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.CREATE,
                    BackupOperationStatus.FAILED,
                    0,
                    e.message ?: "Backup creation failed",
                    errorCode = e.javaClass.simpleName
                )
                notifications.notifyFailed("Backup Failed", e.message ?: "An error occurred during backup creation.")
            } finally {
                secret?.fill('\u0000')
                app.backupSecretVault.remove(request.operationId)
                stopForegroundIfLast(startId)
            }
        }
    }

    private fun runInspectBackup(request: BackupInspectRequest, startId: Int) {
        activeOperationId = request.operationId
        val secret = app.backupSecretVault.take(request.operationId)

        activeJob = scope.launch {
            val sessionId = "inspect_${request.operationId}"
            try {
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.INSPECT,
                    BackupOperationStatus.RUNNING,
                    0,
                    "Reading backup...",
                    sessionId
                )
                startForegroundNow("Inspecting backup", 0, request.operationId)

                val materialized = app.backupPayloadMaterializer.materialize(
                    sessionId = sessionId,
                    sourceUri = request.sourceUri,
                    password = secret,
                    onProgressBytes = { bytes ->
                        // materialize progress
                    }
                )

                val inspection = app.backupArchiveValidator.validate(
                    sessionId = sessionId,
                    file = materialized.zipFile,
                    encrypted = materialized.encrypted,
                    onProgress = { pct ->
                        app.backupOperationStore.updateState(
                            request.operationId,
                            BackupOperationType.INSPECT,
                            BackupOperationStatus.RUNNING,
                            pct,
                            "Validating backup structure...",
                            sessionId
                        )
                        updateNotification("Inspecting backup", pct, "Validating structure...", request.operationId)
                    }
                )

                val preview = RestorePreview(
                    sessionId = sessionId,
                    encrypted = inspection.encrypted,
                    createdAtEpochMs = inspection.manifest.createdAtEpochMs,
                    appVersionName = inspection.manifest.appVersionName,
                    formatVersion = inspection.manifest.formatVersion,
                    transcripts = inspection.manifest.sectionCounts["data/transcripts.json"] ?: 0L,
                    segments = inspection.manifest.sectionCounts["data/transcript_segments.json"] ?: 0L,
                    collections = inspection.manifest.sectionCounts["data/collections.json"] ?: 0L,
                    studyPacks = inspection.manifest.sectionCounts["data/study_packs.json"] ?: 0L,
                    askConversations = inspection.manifest.sectionCounts["data/ask_conversations.json"] ?: 0L,
                    meetingPacks = inspection.manifest.sectionCounts["data/meeting_packs.json"] ?: 0L,
                    speakerClusters = inspection.manifest.sectionCounts["data/speaker_clusters.json"] ?: 0L,
                    mediaCount = inspection.manifest.media.size,
                    mediaBytes = inspection.totalMediaBytes,
                    includesSettings = inspection.manifest.includesSettings,
                    warnings = inspection.validationWarnings
                )

                app.backupOperationStore.setInspection(preview)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.INSPECT,
                    BackupOperationStatus.READY_FOR_RESTORE,
                    100,
                    "Backup ready for preview",
                    sessionId
                )
            } catch (e: InvalidBackupPasswordException) {
                app.restoreWorkspace.cleanup(sessionId)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.INSPECT,
                    BackupOperationStatus.FAILED,
                    0,
                    "Incorrect password or damaged backup.",
                    sessionId,
                    errorCode = "INVALID_PASSWORD"
                )
            } catch (e: CancellationException) {
                app.restoreWorkspace.cleanup(sessionId)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.INSPECT,
                    BackupOperationStatus.CANCELLED,
                    0,
                    "Inspection cancelled",
                    sessionId
                )
            } catch (e: Throwable) {
                app.restoreWorkspace.cleanup(sessionId)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.INSPECT,
                    BackupOperationStatus.FAILED,
                    0,
                    e.message ?: "Failed to inspect backup",
                    sessionId,
                    errorCode = e.javaClass.simpleName
                )
            } finally {
                secret?.fill('\u0000')
                app.backupSecretVault.remove(request.operationId)
                stopForegroundIfLast(startId)
            }
        }
    }

    private fun runRestoreBackup(request: BackupRestoreRequest, startId: Int) {
        activeOperationId = request.operationId

        activeJob = scope.launch {
            try {
                HeavyProcessingArbiter.withLease(HeavyProcessingOwner.BACKUP_RESTORE) {
                    app.backupOperationStore.updateState(
                        request.operationId,
                        BackupOperationType.RESTORE,
                        BackupOperationStatus.RUNNING,
                        0,
                        "Starting restore...",
                        request.sessionId
                    )
                    startForegroundNow("Restoring backup", 0, request.operationId)

                    val payloadZip = app.restoreWorkspace.payloadZip(request.sessionId)
                    val inspection = app.backupArchiveValidator.validate(
                        sessionId = request.sessionId,
                        file = payloadZip,
                        encrypted = false
                    )

                    val result = app.restoreCoordinator.restore(
                        inspection = inspection,
                        mode = request.mode,
                        restoreSettings = request.restoreSettings,
                        onProgress = { pct ->
                            app.backupOperationStore.updateState(
                                request.operationId,
                                BackupOperationType.RESTORE,
                                BackupOperationStatus.RUNNING,
                                pct,
                                "Importing library data...",
                                request.sessionId
                            )
                            updateNotification("Restoring backup", pct, "Importing data ($pct%)...", request.operationId)
                        },
                        cancelled = { activeJob?.isCancelled == true }
                    )

                    app.backupOperationStore.setRestoreResult(result)
                    app.backupOperationStore.updateState(
                        request.operationId,
                        BackupOperationType.RESTORE,
                        BackupOperationStatus.COMPLETED,
                        100,
                        "Restore completed successfully",
                        request.sessionId
                    )
                    notifications.notifyCompleted("Restore Complete", "Transcripts and library data restored.")
                }
            } catch (e: CancellationException) {
                app.restoreWorkspace.cleanup(request.sessionId)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.RESTORE,
                    BackupOperationStatus.CANCELLED,
                    0,
                    "Restore cancelled",
                    request.sessionId
                )
            } catch (e: Throwable) {
                app.restoreWorkspace.cleanup(request.sessionId)
                app.backupOperationStore.updateState(
                    request.operationId,
                    BackupOperationType.RESTORE,
                    BackupOperationStatus.FAILED,
                    0,
                    e.message ?: "Restore failed",
                    request.sessionId,
                    errorCode = e.javaClass.simpleName
                )
                notifications.notifyFailed("Restore Failed", e.message ?: "An error occurred during restore.")
            } finally {
                stopForegroundIfLast(startId)
            }
        }
    }

    private fun startForegroundNow(title: String, progress: Int, opId: String?) {
        val notification = notifications.buildRunning(title, "Processing...", progress, opId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                BackupNotificationFactory.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(BackupNotificationFactory.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, progress: Int, stage: String, opId: String?) {
        val notification = notifications.buildRunning(title, stage, progress, opId)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(BackupNotificationFactory.NOTIFICATION_ID, notification)
    }

    private fun stopForegroundIfLast(startId: Int) {
        activeOperationId = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelfResult(startId)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
