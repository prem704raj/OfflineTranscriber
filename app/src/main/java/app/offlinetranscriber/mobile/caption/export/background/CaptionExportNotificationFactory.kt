package app.offlinetranscriber.mobile.caption.export.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.offlinetranscriber.mobile.MainActivity

class CaptionExportNotificationFactory(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "caption_video_export"
        const val NOTIFICATION_ID = 42016
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Caption Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of local captioned video export"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildProgress(
        text: String,
        progress: Int?,
        jobId: String?
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (progress != null && progress in 0..100) {
            "$text • $progress%"
        } else {
            text
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Offline Transcriber")
            .setContentText(displayText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (progress != null && progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        if (jobId != null) {
            val cancelIntent = Intent(context, CaptionExportForegroundService::class.java).apply {
                action = CaptionExportForegroundService.ACTION_CANCEL
                putExtra(CaptionExportStarter.EXTRA_JOB_ID, jobId)
            }
            val cancelPending = PendingIntent.getService(
                context,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPending
            )
        }

        return builder.build()
    }

    fun notifyProgress(text: String, progress: Int?, jobId: String?) {
        val notification = buildProgress(text, progress, jobId)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun notifyCompleted(jobId: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Offline Transcriber")
            .setContentText("Captioned video ready")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun notifyCancelled() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun notifyFailed() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Offline Transcriber")
            .setContentText("Video export failed")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
