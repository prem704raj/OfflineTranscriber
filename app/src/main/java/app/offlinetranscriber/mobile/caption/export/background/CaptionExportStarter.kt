package app.offlinetranscriber.mobile.caption.export.background

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object CaptionExportStarter {

    const val EXTRA_JOB_ID = "caption_export_job_id"

    fun start(
        context: Context,
        jobId: String
    ) {
        val intent = Intent(context, CaptionExportForegroundService::class.java).apply {
            action = CaptionExportForegroundService.ACTION_RUN
            putExtra(EXTRA_JOB_ID, jobId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel(
        context: Context,
        jobId: String
    ) {
        val intent = Intent(context, CaptionExportForegroundService::class.java).apply {
            action = CaptionExportForegroundService.ACTION_CANCEL
            putExtra(EXTRA_JOB_ID, jobId)
        }
        context.startService(intent)
    }
}
