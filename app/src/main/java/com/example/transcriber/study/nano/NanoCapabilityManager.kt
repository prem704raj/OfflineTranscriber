package com.example.transcriber.study.nano

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NanoCapabilityManager {

    private val model: GenerativeModel =
        Generation.getClient()

    private val _state =
        MutableStateFlow<NanoFeatureState>(
            NanoFeatureState.Checking
        )

    val state = _state.asStateFlow()

    suspend fun refresh(): NanoFeatureState {
        val next = runCatching {
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE ->
                    NanoFeatureState.Available

                FeatureStatus.DOWNLOADABLE ->
                    NanoFeatureState.Downloadable

                FeatureStatus.DOWNLOADING ->
                    NanoFeatureState.Downloading

                else ->
                    NanoFeatureState.Unavailable
            }
        }.getOrElse { error ->
            NanoFeatureState.Error(
                error.message ?: "Unable to check on-device AI."
            )
        }

        _state.value = next
        return next
    }

    suspend fun download() {
        val current = refresh()

        if (current !is NanoFeatureState.Downloadable) {
            return
        }

        _state.value = NanoFeatureState.Downloading

        runCatching {
            model.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadStarted -> {
                        _state.value =
                            NanoFeatureState.Downloading
                    }

                    is DownloadStatus.DownloadProgress -> {
                        _state.value =
                            NanoFeatureState.Downloading
                    }

                    DownloadStatus.DownloadCompleted -> {
                        _state.value =
                            NanoFeatureState.Available
                    }

                    is DownloadStatus.DownloadFailed -> {
                        _state.value =
                            NanoFeatureState.Error(
                                status.e.message
                                    ?: "On-device AI download failed."
                            )
                    }
                }
            }
        }.onFailure { error ->
            _state.value = NanoFeatureState.Error(
                error.message ?: "On-device AI download failed."
            )
        }
    }

    fun model(): GenerativeModel = model

    fun close() {
        model.close()
    }
}
