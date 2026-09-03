package app.offlinetranscriber.mobile.background

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.offlinetranscriber.mobile.TranscriberApplication

object BackgroundTranscriptionStarter {

    fun start(
        context: Context
    ) {
        val intent = Intent(
            context,
            TranscriptionForegroundService::class.java
        ).setAction(
            TranscriptionForegroundService.ACTION_DRAIN_QUEUE
        )

        ContextCompat.startForegroundService(
            context,
            intent
        )
    }

    /**
     * Call only while a visible Activity is starting/resuming.
     * Do not call this from arbitrary background receivers.
     */
    suspend fun resumeIfNeededFromVisibleUi(
        context: Context
    ) {
        val app = context.applicationContext as TranscriberApplication

        app.queueManager.recoverInterrupted()

        if (app.queueRepository.hasUnfinished()) {
            start(context)
        }
    }
}
