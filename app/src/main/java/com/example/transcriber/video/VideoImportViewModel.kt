package com.example.transcriber.video

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@UnstableApi
class VideoImportViewModel(application: Application) : AndroidViewModel(application) {
    private val extractor = VideoAudioExtractor(application)
    private val _state = MutableStateFlow<VideoPreparationState>(VideoPreparationState.Idle)
    val state = _state.asStateFlow()

    fun prepareVideo(uri: Uri) {
        val app = getApplication<Application>()
        val displayName = app.displayNameFor(uri)
        _state.value = VideoPreparationState.Preparing(displayName, null)

        extractor.extract(
            inputVideoUri = uri,
            onProgress = { progress ->
                _state.value = VideoPreparationState.Preparing(displayName, progress)
            },
            onSuccess = { audioUri ->
                _state.value = VideoPreparationState.Ready(
                    sourceVideoUri = uri,
                    extractedAudioUri = audioUri,
                    displayName = displayName
                )
            },
            onError = { error ->
                _state.value = VideoPreparationState.Error(
                    error.message ?: "Unable to prepare this video."
                )
            }
        )
    }

    fun cancel() {
        extractor.cancel()
        _state.value = VideoPreparationState.Idle
    }

    fun reset() {
        _state.value = VideoPreparationState.Idle
    }

    override fun onCleared() {
        extractor.cancel()
        super.onCleared()
    }
}
