package com.example.transcriber.domain.model

import android.net.Uri
import com.example.transcriber.data.model.MediaType

data class TranscriptionRequest(
    val audioUri: Uri,
    val sourceUri: Uri = audioUri,
    val mediaType: String = MediaType.AUDIO,
    val suggestedTitle: String? = null,
    val audioFileName: String? = null,
    val language: String = "auto",
    val translate: Boolean = false
)
