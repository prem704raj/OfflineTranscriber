package com.example.transcriber.speaker.background

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.speaker.model.SpeakerCountMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpeakerDiarizationForegroundService : Service() {

    companion object {
        const val ACTION_START_DIARIZATION = "com.example.transcriber.action.START_SPEAKER_DIARIZATION"
        const val ACTION_CANCEL = "com.example.transcriber.action.CANCEL_SPEAKER_DIARIZATION"

        const val EXTRA_TRANSCRIPT_ID = "extra_transcript_id"
        const val EXTRA_AUDIO_URI = "extra_audio_uri"
        const val EXTRA_COUNT_MODE = "extra_count_mode"
        const val EXTRA_REQUESTED_COUNT = "extra_requested_count"
    }

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Default)

    private lateinit var app: TranscriberApplication
    private lateinit var notifications: SpeakerDiarizationNotificationFactory

    private var activeJob: Job? = null
    private var currentTranscriptId: Long? = null

    override fun onCreate() {
        super.onCreate()
        app = application as TranscriberApplication
        notifications = SpeakerDiarizationNotificationFactory(this)
        notifications.ensureChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                activeJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_DIARIZATION -> {
                val transcriptId = intent.getLongExtra(EXTRA_TRANSCRIPT_ID, -1L)
                val audioUriString = intent.getStringExtra(EXTRA_AUDIO_URI)
                val modeStr = intent.getStringExtra(EXTRA_COUNT_MODE) ?: SpeakerCountMode.AUTO.name
                val reqCount = intent.getIntExtra(EXTRA_REQUESTED_COUNT, -1).takeIf { it > 0 }

                if (transcriptId > 0 && !audioUriString.isNullOrBlank()) {
                    currentTranscriptId = transcriptId
                    val audioUri = Uri.parse(audioUriString)
                    val mode = runCatching { SpeakerCountMode.valueOf(modeStr) }.getOrDefault(SpeakerCountMode.AUTO)
                    startDiarization(transcriptId, audioUri, mode, reqCount)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            else -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDiarization(
        transcriptId: Long,
        audioUri: Uri,
        mode: SpeakerCountMode,
        requestedCount: Int?
    ) {
        if (activeJob?.isActive == true) return

        activeJob = scope.launch {
            try {
                app.speakerCoordinator.runDiarization(
                    transcriptId = transcriptId,
                    audioUri = audioUri,
                    countMode = mode,
                    requestedSpeakerCount = requestedCount
                ) { progress ->
                    runCatching {
                        NotificationManagerCompat.from(this@SpeakerDiarizationForegroundService).notify(
                            SpeakerDiarizationNotificationFactory.NOTIFICATION_ID,
                            notifications.build(progress)
                        )
                    }
                }
            } catch (_: CancellationException) {
                // User cancelled or service destroyed
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startAsForeground() {
        val initial = notifications.build(0)
        val type = if (Build.VERSION.SDK_INT >= 35) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            SpeakerDiarizationNotificationFactory.NOTIFICATION_ID,
            initial,
            type
        )
    }

    override fun onDestroy() {
        activeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
