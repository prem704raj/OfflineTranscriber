package app.offlinetranscriber.mobile.domain.model

import android.net.Uri
import app.offlinetranscriber.mobile.data.model.MediaType

data class TranscriptionRequest(
    val audioUri: Uri,
    val sourceUri: Uri = audioUri,
    val mediaType: String = MediaType.AUDIO,
    val suggestedTitle: String? = null,
    val audioFileName: String? = null,
    val language: String = "auto",
    val translate: Boolean = false
)
