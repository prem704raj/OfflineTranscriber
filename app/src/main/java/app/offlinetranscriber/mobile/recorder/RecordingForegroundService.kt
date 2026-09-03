package app.offlinetranscriber.mobile.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.offlinetranscriber.mobile.MainActivity
import app.offlinetranscriber.mobile.R
import app.offlinetranscriber.mobile.shortcuts.ShortcutActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class RecordingForegroundService : Service() {

    companion object {
        private const val TAG = "RecordingFGS"
        const val CHANNEL_ID = "recording_service_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "app.offlinetranscriber.mobile.action.START_RECORDING"
        const val ACTION_PAUSE = "app.offlinetranscriber.mobile.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "app.offlinetranscriber.mobile.action.RESUME_RECORDING"
        const val ACTION_STOP = "app.offlinetranscriber.mobile.action.STOP_RECORDING"
        const val ACTION_DISCARD = "app.offlinetranscriber.mobile.action.DISCARD_RECORDING"

        fun startIntent(context: Context): Intent =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_START
            }

        fun pauseIntent(context: Context): Intent =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }

        fun resumeIntent(context: Context): Intent =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_RESUME
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_STOP
            }

        fun discardIntent(context: Context): Intent =
            Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_DISCARD
            }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    private var startTimeMs = 0L
    private var accumulatedMs = 0L
    private var isRecording = false
    private var isPaused = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
            ACTION_DISCARD -> handleDiscard()
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_NOT_STICKY
    }

    private fun handleStart() {
        if (isRecording) return

        try {
            val dir = File(
                getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "recordings"
            ).apply { mkdirs() }

            val file = File(dir, "recording_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            isRecording = true
            isPaused = false
            startTimeMs = System.currentTimeMillis()
            accumulatedMs = 0L

            RecordingSessionStore.set(
                RecordingState(
                    status = RecordingStatus.RECORDING,
                    durationMs = 0L,
                    amplitude = 0f,
                    filePath = file.absolutePath
                )
            )

            val notification = buildNotification(0L, isPaused = false)
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
            )

            startTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            handleError("Failed to initialize microphone: ${e.message}")
        }
    }

    private fun handlePause() {
        if (!isRecording || isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                accumulatedMs += (System.currentTimeMillis() - startTimeMs)
                isPaused = true

                RecordingSessionStore.update {
                    it.copy(status = RecordingStatus.PAUSED, amplitude = 0f)
                }

                updateNotification(accumulatedMs, isPaused = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recording", e)
            }
        }
    }

    private fun handleResume() {
        if (!isRecording || !isPaused) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                startTimeMs = System.currentTimeMillis()
                isPaused = false

                RecordingSessionStore.update {
                    it.copy(status = RecordingStatus.RECORDING)
                }

                updateNotification(currentDuration(), isPaused = false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recording", e)
            }
        }
    }

    private fun handleStop() {
        if (!isRecording) {
            stopSelf()
            return
        }

        tickerJob?.cancel()
        val finalDuration = currentDuration()

        runCatching {
            recorder?.apply {
                stop()
                release()
            }
        }
        recorder = null
        isRecording = false
        isPaused = false

        val finalPath = outputFile?.absolutePath
        RecordingSessionStore.set(
            RecordingState(
                status = RecordingStatus.COMPLETED,
                durationMs = finalDuration,
                amplitude = 0f,
                filePath = finalPath
            )
        )

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleDiscard() {
        tickerJob?.cancel()
        runCatching {
            recorder?.apply {
                stop()
                release()
            }
        }
        recorder = null
        isRecording = false
        isPaused = false

        outputFile?.delete()
        outputFile = null

        RecordingSessionStore.reset()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handleError(message: String) {
        tickerJob?.cancel()
        runCatching {
            recorder?.release()
        }
        recorder = null
        isRecording = false
        isPaused = false

        outputFile?.delete()
        outputFile = null

        RecordingSessionStore.set(
            RecordingState(
                status = RecordingStatus.ERROR,
                errorMessage = message
            )
        )
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun currentDuration(): Long {
        return if (isRecording && !isPaused) {
            accumulatedMs + (System.currentTimeMillis() - startTimeMs)
        } else {
            accumulatedMs
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            var lastNotificationSecond = -1L
            while (isActive && isRecording) {
                val dur = currentDuration()
                val amp = if (!isPaused) {
                    val maxAmp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    (maxAmp / 32767f).coerceIn(0f, 1f)
                } else {
                    0f
                }

                RecordingSessionStore.update {
                    it.copy(durationMs = dur, amplitude = amp)
                }

                val currentSec = dur / 1000L
                if (currentSec != lastNotificationSecond) {
                    lastNotificationSecond = currentSec
                    updateNotification(dur, isPaused)
                }

                delay(100)
            }
        }
    }

    private fun updateNotification(durationMs: Long, isPaused: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(durationMs, isPaused))
    }

    private fun buildNotification(durationMs: Long, isPaused: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ShortcutActions.RECORD
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDuration = formatDuration(durationMs)
        val title = if (isPaused) "Recording Paused" else "Recording Audio"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(formattedDuration)
            .setSmallIcon(R.drawable.ic_shortcut_record)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isPaused) {
                val resumePendingIntent = PendingIntent.getService(
                    this,
                    2,
                    resumeIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    "Resume",
                    resumePendingIntent
                )
            } else {
                val pausePendingIntent = PendingIntent.getService(
                    this,
                    3,
                    pauseIntent(this),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    pausePendingIntent
                )
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for live microphone recording."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000L
        val seconds = totalSec % 60
        val minutes = (totalSec / 60) % 60
        val hours = totalSec / 3600
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        runCatching {
            recorder?.release()
        }
        recorder = null
    }
}
