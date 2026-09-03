package app.offlinetranscriber.mobile.caption.export.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.processing.HeavyProcessingArbiter
import app.offlinetranscriber.mobile.processing.HeavyProcessingOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CaptionExportForegroundService : Service() {

    companion object {
        const val ACTION_RUN = "app.offlinetranscriber.mobile.caption.EXPORT_RUN"
        const val ACTION_CANCEL = "app.offlinetranscriber.mobile.caption.EXPORT_CANCEL"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: TranscriberApplication
    private lateinit var notifications: CaptionExportNotificationFactory

    private var activeJob: Job? = null
    private var activeJobId: String? = null

    override fun onCreate() {
        super.onCreate()
        app = application as TranscriberApplication
        notifications = CaptionExportNotificationFactory(this)
        notifications.ensureChannel()

        startForegroundNow("Preparing video export", null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val jobId = intent?.getStringExtra(CaptionExportStarter.EXTRA_JOB_ID)

        when (intent?.action) {
            ACTION_CANCEL -> {
                if (jobId != null && jobId == activeJobId) {
                    activeJob?.cancel()
                }
            }
            ACTION_RUN -> {
                if (jobId != null) {
                    runJob(jobId, startId)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun runJob(jobId: String, startId: Int) {
        if (activeJob?.isActive == true) {
            return
        }

        activeJobId = jobId

        activeJob = scope.launch {
            try {
                val currentJob = app.captionExportJobStore.get(jobId)
                    ?: error("Export job $jobId not found.")

                app.captionExportJobStore.put(
                    currentJob.copy(
                        status = CaptionExportJobStatus.WAITING,
                        progress = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                updateNotification("Waiting for other processing", null)

                HeavyProcessingArbiter.withLease(HeavyProcessingOwner.VIDEO_EXPORT) {
                    execute(jobId)
                }

            } catch (ce: CancellationException) {
                app.captionExportCoordinator.cancelAndCleanup(jobId)
                app.captionExportJobStore.get(jobId)?.let { current ->
                    app.captionExportJobStore.put(
                        current.copy(
                            status = CaptionExportJobStatus.CANCELLED,
                            progress = 0,
                            errorMessage = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                notifications.notifyCancelled()
            } catch (error: Throwable) {
                app.captionExportCoordinator.cancelAndCleanup(jobId)
                app.captionExportJobStore.get(jobId)?.let { current ->
                    app.captionExportJobStore.put(
                        current.copy(
                            status = CaptionExportJobStatus.FAILED,
                            progress = 0,
                            errorMessage = safeMessage(error),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                notifications.notifyFailed()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                activeJobId = null
            }
        }
    }

    private suspend fun execute(jobId: String) {
        val currentJob = app.captionExportJobStore.get(jobId)
            ?: error("Export job $jobId missing.")

        app.captionExportJobStore.put(
            currentJob.copy(
                status = CaptionExportJobStatus.EXPORTING,
                progress = 0,
                updatedAt = System.currentTimeMillis()
            )
        )

        val ready = app.captionExportCoordinator.execute(
            jobId = jobId,
            onProgress = { progress ->
                app.captionExportJobStore.get(jobId)?.let { current ->
                    app.captionExportJobStore.put(
                        current.copy(
                            status = CaptionExportJobStatus.EXPORTING,
                            progress = progress,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                updateNotification("Exporting captioned video", progress)
            },
            cancelled = {
                activeJob?.isActive != true
            }
        )

        app.captionExportJobStore.get(jobId)?.let { current ->
            app.captionExportJobStore.put(
                current.copy(
                    status = CaptionExportJobStatus.COMPLETED,
                    progress = 100,
                    readyFilePath = ready.absolutePath,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        notifications.notifyCompleted(jobId)
    }

    private fun updateNotification(text: String, progress: Int?) {
        notifications.notifyProgress(text, progress, activeJobId)
    }

    private fun startForegroundNow(text: String, progress: Int?) {
        val notification = notifications.buildProgress(text, progress, null)
        val type = if (Build.VERSION.SDK_INT >= 35) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            CaptionExportNotificationFactory.NOTIFICATION_ID,
            notification,
            type
        )
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        activeJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    override fun onDestroy() {
        activeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun safeMessage(error: Throwable): String =
        when (error) {
            is androidx.media3.transformer.ExportException ->
                "This video couldn't be exported on this device."
            else ->
                "Video export failed."
        }
}
