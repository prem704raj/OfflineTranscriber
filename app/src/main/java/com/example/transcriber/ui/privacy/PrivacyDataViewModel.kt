package com.example.transcriber.ui.privacy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.storage.StorageUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivacyDataUiState(
    val storage: StorageUsage? = null,
    val busy: Boolean = false,
    val message: String? = null
)

class PrivacyDataViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication

    private val _state = MutableStateFlow(PrivacyDataUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(
            storage = app.storageManager.usage()
        )
    }

    fun cleanTemp() {
        runAction(success = "Temporary files cleaned.") {
            app.dataDeletionCoordinator.cleanTemporaryFiles()
        }
    }

    fun deleteTranscriptionContent() {
        runAction(success = "Transcription content deleted.") {
            app.dataDeletionCoordinator.deleteTranscriptionContent()
        }
    }

    fun deleteModels() {
        runAction(success = "Downloaded models deleted.") {
            app.dataDeletionCoordinator.deleteAllModels()
        }
    }

    fun resetPreferences() {
        runAction(success = "Preferences reset.") {
            app.dataDeletionCoordinator.resetPreferences()
        }
    }

    fun deleteAllLocalData(typedConfirmation: String) {
        if (typedConfirmation != "DELETE") {
            _state.value = _state.value.copy(
                message = "Type DELETE exactly to confirm."
            )
            return
        }

        runAction(success = "All local app data deleted.") {
            app.dataDeletionCoordinator.deleteEverythingExceptBillingCache()
            app.entitlementStore.clearLocalCache()
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun runAction(
        success: String,
        block: suspend () -> Unit
    ) {
        if (_state.value.busy) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                busy = true,
                message = null
            )

            runCatching {
                block()
            }.onSuccess {
                _state.value = _state.value.copy(
                    busy = false,
                    storage = app.storageManager.usage(),
                    message = success
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    busy = false,
                    message = it.message ?: "Operation failed."
                )
            }
        }
    }
}
