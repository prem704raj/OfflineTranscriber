package app.offlinetranscriber.mobile.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.offlinetranscriber.mobile.MainActivity
import app.offlinetranscriber.mobile.R
import app.offlinetranscriber.mobile.queue.QueueRuntimeState
import app.offlinetranscriber.mobile.queue.TranscriptionJobStage
import app.offlinetranscriber.mobile.shortcuts.ShortcutActions

class TranscriptionNotificationFactory(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "transcription_processing"
        const val NOTIFICATION_ID = 5102
    }

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transcription processing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progress for active offline transcription."
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        manager?.createNotificationChannel(channel)
    }

    fun build(
        runtime: QueueRuntimeState
    ): Notification {
        val running = runtime as? QueueRuntimeState.Running
        val progress = running?.progress?.coerceIn(0, 99) ?: 0
        val stage = running?.stage

        val text = when (stage) {
            TranscriptionJobStage.PREPARING -> "Preparing video • $progress%"
            TranscriptionJobStage.TRANSCRIBING -> "Transcribing • $progress%"
            null -> "Preparing transcription"
        }

        val queueSuffix = running
            ?.queuedAfterCurrent
            ?.takeIf { it > 0 }
            ?.let { " • $it more waiting" }
            .orEmpty()

        val openIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            action = ShortcutActions.OPEN_QUEUE
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPending = PendingIntent.getActivity(
            context,
            5102,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(
            context,
            TranscriptionForegroundService::class.java
        ).apply {
            action = TranscriptionForegroundService.ACTION_CANCEL_CURRENT
        }

        val cancelPending = PendingIntent.getService(
            context,
            5103,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification_transcription)
            .setContentTitle("Offline Transcriber")
            .setContentText(text + queueSuffix)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setProgress(100, progress, running == null)
            .addAction(0, "Open queue", openPending)
            .addAction(0, "Cancel", cancelPending)
            .build()
    }
}
