package com.example.transcriber.speaker.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.transcriber.MainActivity
import com.example.transcriber.R

class SpeakerDiarizationNotificationFactory(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "speaker_diarization_channel"
        const val NOTIFICATION_ID = 5105
    }

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Speaker Intelligence",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progress for offline speaker detection."
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        manager?.createNotificationChannel(channel)
    }

    fun build(progress: Int): Notification {
        val openIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPending = PendingIntent.getActivity(
            context,
            5105,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(
            context,
            SpeakerDiarizationForegroundService::class.java
        ).apply {
            action = SpeakerDiarizationForegroundService.ACTION_CANCEL
        }

        val cancelPending = PendingIntent.getService(
            context,
            5106,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressClamped = progress.coerceIn(0, 100)
        val text = when {
            progressClamped < 20 -> "Preparing audio • $progressClamped%"
            progressClamped < 85 -> "Analyzing speakers • $progressClamped%"
            progressClamped < 100 -> "Aligning transcript • $progressClamped%"
            else -> "Speaker analysis complete"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_transcription)
            .setContentTitle("Speaker Intelligence")
            .setContentText(text)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setProgress(100, progressClamped, progressClamped == 0)
            .addAction(0, "Cancel", cancelPending)
            .build()
    }
}
