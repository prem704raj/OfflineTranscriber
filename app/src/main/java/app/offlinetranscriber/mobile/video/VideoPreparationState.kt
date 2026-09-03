package app.offlinetranscriber.mobile.video

import android.net.Uri

sealed interface VideoPreparationState {
    data object Idle : VideoPreparationState

    data class Preparing(
        val displayName: String,
        val progressPercent: Int?
    ) : VideoPreparationState

    data class Ready(
        val sourceVideoUri: Uri,
        val extractedAudioUri: Uri,
        val displayName: String
    ) : VideoPreparationState

    data class Error(
        val message: String
    ) : VideoPreparationState
}
