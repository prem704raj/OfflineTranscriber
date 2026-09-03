package app.offlinetranscriber.mobile.speaker.background

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import app.offlinetranscriber.mobile.speaker.model.SpeakerCountMode

object SpeakerDiarizationStarter {

    fun start(
        context: Context,
        transcriptId: Long,
        audioUri: Uri,
        countMode: SpeakerCountMode = SpeakerCountMode.AUTO,
        requestedSpeakerCount: Int? = null
    ) {
        val intent = Intent(context, SpeakerDiarizationForegroundService::class.java).apply {
            action = SpeakerDiarizationForegroundService.ACTION_START_DIARIZATION
            putExtra(SpeakerDiarizationForegroundService.EXTRA_TRANSCRIPT_ID, transcriptId)
            putExtra(SpeakerDiarizationForegroundService.EXTRA_AUDIO_URI, audioUri.toString())
            putExtra(SpeakerDiarizationForegroundService.EXTRA_COUNT_MODE, countMode.name)
            requestedSpeakerCount?.let {
                putExtra(SpeakerDiarizationForegroundService.EXTRA_REQUESTED_COUNT, it)
            }
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancel(context: Context) {
        val intent = Intent(context, SpeakerDiarizationForegroundService::class.java).apply {
            action = SpeakerDiarizationForegroundService.ACTION_CANCEL
        }
        context.startService(intent)
    }
}
