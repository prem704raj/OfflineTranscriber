package com.example.transcriber.meeting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.study.nano.NanoCapabilityManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MeetingInsightsViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val meetingRepository = app.meetingRepository
    private val transcriptRepository = app.transcriptRepository
    private val entitlementRepository = app.entitlementRepository

    private val nanoManager = NanoCapabilityManager()
    private val coordinator = MeetingGenerationCoordinator(
        model = nanoManager.model()
    )

    private val transcriptId: Long = savedStateHandle.get<Long>("transcriptId") ?: 0L

    private val initialTab: MeetingInsightsTab = runCatching {
        savedStateHandle.get<String>("selectedTab")?.let { MeetingInsightsTab.valueOf(it) }
    }.getOrNull() ?: MeetingInsightsTab.OVERVIEW

    private val _uiState = MutableStateFlow(
        MeetingInsightsUiState(
            selectedTab = initialTab
        )
    )
    val uiState: StateFlow<MeetingInsightsUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            transcriptRepository.observeTranscript(transcriptId).collect { transcript ->
                _uiState.update { it.copy(transcript = transcript) }
            }
        }

        viewModelScope.launch {
            meetingRepository.observe(transcriptId).collect { pack ->
                _uiState.update { it.copy(pack = pack) }
            }
        }

        viewModelScope.launch {
            nanoManager.state.collect { nanoState ->
                _uiState.update { it.copy(nanoState = nanoState) }
            }
        }

        viewModelScope.launch {
            nanoManager.refresh()
        }
    }

    fun selectTab(tab: MeetingInsightsTab) {
        savedStateHandle["selectedTab"] = tab.name
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setForeground(isForeground: Boolean) {
        _uiState.update { it.copy(foreground = isForeground) }
    }

    fun generate(onProRequired: () -> Unit) {
        if (generationJob?.isActive == true) return

        generationJob = viewModelScope.launch {
            val entitlement = entitlementRepository.entitlement.first()

            if (entitlement != Entitlement.PRO) {
                onProRequired()
                return@launch
            }

            val segments = meetingRepository.sourceSegments(transcriptId)
            if (segments.isEmpty()) {
                _uiState.update { it.copy(message = "This transcript has no text to analyze.") }
                return@launch
            }

            _uiState.update { it.copy(generating = true, progress = 0) }

            val result = runCatching {
                coordinator.generate(
                    segments = segments,
                    allowNano = _uiState.value.foreground,
                    onProgress = { percent ->
                        _uiState.update { it.copy(progress = percent) }
                    }
                )
            }

            result.onSuccess { generatedPack ->
                meetingRepository.replaceGeneratedPack(transcriptId, generatedPack)
                _uiState.update { it.copy(generating = false, progress = 100) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        generating = false,
                        message = error.message ?: "Unable to create meeting insights."
                    )
                }
            }
        }
    }

    fun toggleAction(actionId: Long, done: Boolean) {
        viewModelScope.launch {
            meetingRepository.setActionDone(actionId, done)
        }
    }

    fun editAction(actionId: Long, text: String, assignee: String, dueText: String) {
        viewModelScope.launch {
            runCatching {
                meetingRepository.editAction(actionId, text, assignee, dueText)
            }.onFailure { error ->
                _uiState.update { it.copy(message = error.message ?: "Unable to update action.") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
