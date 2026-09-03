package app.offlinetranscriber.mobile.backup.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.offlinetranscriber.mobile.MainActivity
import app.offlinetranscriber.mobile.R

class BackupNotificationFactory(
    private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "backup_restore_operations"
        const val NOTIFICATION_ID = 4001
    }

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup and Restore",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of backup creation, inspection and restoration"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildRunning(
        title: String,
        stage: String,
        progress: Int,
        operationId: String?
    ): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(stage.ifBlank { "Processing..." })
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        if (operationId != null) {
            val cancelIntent = Intent(context, BackupRestoreForegroundService::class.java).apply {
                action = BackupRestoreForegroundService.ACTION_CANCEL
                putExtra(BackupRestoreForegroundService.EXTRA_OPERATION_ID, operationId)
            }
            val cancelPendingIntent = PendingIntent.getService(
                context,
                2,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        return builder.build()
    }

    fun notifyCompleted(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    fun notifyFailed(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID + 2, notification)
    }
}
