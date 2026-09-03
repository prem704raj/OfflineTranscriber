package app.offlinetranscriber.mobile.ui.modelmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.background.BackgroundTranscriptionStarter
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.modelmanager.ModelCatalog
import app.offlinetranscriber.mobile.modelmanager.ModelDownloadState
import app.offlinetranscriber.mobile.modelmanager.WhisperModelSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ModelRowState(
    val spec: WhisperModelSpec,
    val installed: Boolean,
    val selected: Boolean,
    val recommended: Boolean,
    val proRequired: Boolean
)

data class ModelManagerUiState(
    val rows: List<ModelRowState> = emptyList(),
    val download: ModelDownloadState = ModelDownloadState.Idle,
    val entitlement: Entitlement = Entitlement.FREE,
    val openPaywallFeature: ProFeature? = null,
    val message: String? = null
)

class ModelManagerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app =
        application as TranscriberApplication

    private val manager =
        app.modelManager

    private val settings =
        app.settingsRepository

    private val entitlementRepo =
        app.entitlementRepository

    private val _state =
        MutableStateFlow(
            ModelManagerUiState()
        )

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settings.settings,
                manager.downloadState,
                entitlementRepo.entitlement
            ) { prefs, download, entitlement ->
                Triple(prefs, download, entitlement)
            }.collect { (prefs, download, entitlement) ->
                val recommended =
                    app.deviceModelAdvisor
                        .recommend()

                _state.value = _state.value.copy(
                    rows = ModelCatalog.all.map { spec ->
                        ModelRowState(
                            spec = spec,
                            installed = manager.isInstalled(spec),
                            selected = prefs.selectedModelId == spec.id,
                            recommended = recommended.id == spec.id,
                            proRequired = spec.id == ModelCatalog.accurate.id
                        )
                    },
                    download = download,
                    entitlement = entitlement
                )

                if (download is ModelDownloadState.Completed) {
                    if (app.queueRepository.hasUnfinished()) {
                        BackgroundTranscriptionStarter.start(application)
                    }
                }
            }
        }
    }

    fun download(
        spec: WhisperModelSpec
    ) {
        viewModelScope.launch {
            val entitlement = entitlementRepo.entitlement.first()
            if (spec.id == ModelCatalog.accurate.id && entitlement != Entitlement.PRO) {
                _state.value = _state.value.copy(
                    openPaywallFeature = ProFeature.ACCURATE_MODEL
                )
                return@launch
            }
            manager.download(spec)
        }
    }

    fun cancelDownload() {
        manager.cancelDownload()
    }

    fun select(
        spec: WhisperModelSpec
    ) {
        viewModelScope.launch {
            val entitlement = entitlementRepo.entitlement.first()
            if (spec.id == ModelCatalog.accurate.id && entitlement != Entitlement.PRO) {
                _state.value = _state.value.copy(
                    openPaywallFeature = ProFeature.ACCURATE_MODEL
                )
                return@launch
            }
            runCatching {
                manager.select(spec)
            }.onFailure {
                _state.value =
                    _state.value.copy(
                        message = it.message
                    )
            }
        }
    }

    fun delete(
        spec: WhisperModelSpec
    ) {
        viewModelScope.launch {
            runCatching {
                manager.delete(spec)
            }.onFailure {
                _state.value =
                    _state.value.copy(
                        message = it.message
                    )
            }
        }
    }

    fun clearPaywallTrigger() {
        _state.value = _state.value.copy(
            openPaywallFeature = null
        )
    }

    fun clearMessage() {
        _state.value =
            _state.value.copy(
                message = null
            )
    }
}
