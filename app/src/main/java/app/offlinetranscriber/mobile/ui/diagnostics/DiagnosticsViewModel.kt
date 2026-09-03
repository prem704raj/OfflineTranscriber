package app.offlinetranscriber.mobile.ui.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.diagnostics.AppDiagnostics
import app.offlinetranscriber.mobile.study.nano.NanoCapabilityManager
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val loading: Boolean = true,
    val diagnostics: AppDiagnostics? = null,
    val message: String? = null
)

class DiagnosticsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val nano = NanoCapabilityManager()

    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)

            val nanoStatus = runCatching {
                when (nano.refresh()) {
                    is NanoFeatureState.Available -> "AVAILABLE"
                    is NanoFeatureState.Downloadable -> "DOWNLOADABLE"
                    is NanoFeatureState.Downloading -> "DOWNLOADING"
                    is NanoFeatureState.Checking -> "CHECKING"
                    is NanoFeatureState.Unavailable -> "UNAVAILABLE"
                    is NanoFeatureState.Error -> "ERROR"
                }
            }.getOrDefault("UNKNOWN")

            runCatching {
                app.diagnosticsRepository.collect(nanoStatus)
            }.onSuccess {
                _state.value = DiagnosticsUiState(
                    loading = false,
                    diagnostics = it
                )
            }.onFailure {
                _state.value = DiagnosticsUiState(
                    loading = false,
                    message = it.message ?: "Diagnostics unavailable."
                )
            }
        }
    }

    override fun onCleared() {
        nano.close()
        super.onCleared()
    }
}
