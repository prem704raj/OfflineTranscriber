package com.example.transcriber.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.queue.QueueRuntimeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TranscriptionForegroundService : Service() {

    companion object {
        const val ACTION_DRAIN_QUEUE = "com.example.transcriber.action.DRAIN_QUEUE"
        const val ACTION_CANCEL_CURRENT = "com.example.transcriber.action.CANCEL_CURRENT"
    }

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Default)

    private lateinit var app: TranscriberApplication
    private lateinit var notifications: TranscriptionNotificationFactory

    private var drainJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        app = application as TranscriberApplication
        notifications = TranscriptionNotificationFactory(this)
        notifications.ensureChannel()

        startAsForeground()

        scope.launch {
            app.queueManager.runtime.collectLatest { state ->
                runCatching {
                    NotificationManagerCompat.from(this@TranscriptionForegroundService).notify(
                        TranscriptionNotificationFactory.NOTIFICATION_ID,
                        notifications.build(state)
                    )
                }
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_CANCEL_CURRENT -> {
                scope.launch {
                    val current = (app.queueManager.runtime.value as? QueueRuntimeState.Running)?.jobId
                    if (current != null) {
                        app.queueManager.cancel(current)
                    }
                }
            }

            ACTION_DRAIN_QUEUE,
            null -> {
                startDrainIfNeeded()
            }
        }

        return START_NOT_STICKY
    }

    private fun startDrainIfNeeded() {
        if (drainJob?.isActive == true) return

        drainJob = scope.launch {
            try {
                app.queueManager.runUntilIdle()
            } catch (_: CancellationException) {
                // service/system stop
            } finally {
                if (!app.queueRepository.hasUnfinished()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun startAsForeground() {
        val initial = notifications.build(QueueRuntimeState.Idle)

        val type = if (Build.VERSION.SDK_INT >= 35) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            TranscriptionNotificationFactory.NOTIFICATION_ID,
            initial,
            type
        )
    }

    /**
     * API 35+ dataSync/mediaProcessing timeout callback.
     */
    override fun onTimeout(
        startId: Int,
        fgsType: Int
    ) {
        scope.launch {
            app.queueManager.requeueCurrentForSystemStop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    override fun onTaskRemoved(
        rootIntent: Intent?
    ) {
        // Intentionally do not stop.
        // User-visible foreground processing may continue.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (app.queueManager.runtime.value is QueueRuntimeState.Running) {
            app.queueManager.cancelNativeWorkForServiceDestroy()
        }

        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
