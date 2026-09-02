package com.example.transcriber.ui.speaker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.data.model.SpeakerClusterEntity
import com.example.transcriber.speaker.background.SpeakerDiarizationStarter
import com.example.transcriber.speaker.model.SpeakerCountMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpeakerViewModel(
    application: Application,
    private val transcriptId: Long
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val repository = app.speakerRepository
    private val modelManager = app.speakerModelManager
    private val billing = app.billingRepository
    private val transcriptRepo = app.transcriptRepository

    private val _selectedFilterId = MutableStateFlow<Long?>(null)
    private val _renameDialogCluster = MutableStateFlow<SpeakerClusterEntity?>(null)
    private val _mergeDialogCluster = MutableStateFlow<SpeakerClusterEntity?>(null)
    private val _showCountDialog = MutableStateFlow(false)
    private val _showSetupDialog = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SpeakerScreenUiState> = combine(
        repository.observeRun(transcriptId),
        repository.observeClusters(transcriptId),
        repository.observeTurns(transcriptId),
        repository.observeAssignments(transcriptId),
        modelManager.downloadState,
        _selectedFilterId,
        _renameDialogCluster,
        _mergeDialogCluster,
        _showCountDialog,
        _showSetupDialog,
        _errorMessage
    ) { args ->
        val run = args[0] as com.example.transcriber.data.model.SpeakerDiarizationRunEntity?
        @Suppress("UNCHECKED_CAST")
        val clusters = args[1] as List<SpeakerClusterEntity>
        @Suppress("UNCHECKED_CAST")
        val turns = args[2] as List<com.example.transcriber.data.database.SpeakerTurnRow>
        @Suppress("UNCHECKED_CAST")
        val assignments = args[3] as List<com.example.transcriber.data.database.SpeakerAssignmentRow>
        val downloadState = args[4] as com.example.transcriber.speaker.modelmanager.SpeakerModelDownloadState
        val filterId = args[5] as Long?
        val renameCluster = args[6] as SpeakerClusterEntity?
        val mergeCluster = args[7] as SpeakerClusterEntity?
        val showCount = args[8] as Boolean
        val showSetup = args[9] as Boolean
        val errorMsg = args[10] as String?

        val isPro = com.example.transcriber.billing.FeatureAccessPolicy.hasAccess(billing.state.value.entitlement, ProFeature.SPEAKER_INTELLIGENCE)
        val areModelsReady = modelManager.areModelsReady()

        // Compute speaker cluster stats
        val totalAudioSpeakingMs = turns.sumOf { maxOf(0L, it.endMs - it.startMs) }
        val turnsByCluster = turns.groupBy { it.speakerClusterId }

        val stats = clusters.map { cluster ->
            val clusterTurns = turnsByCluster[cluster.id] ?: emptyList()
            val speakingMs = clusterTurns.sumOf { maxOf(0L, it.endMs - it.startMs) }
            val pct = if (totalAudioSpeakingMs > 0L) {
                speakingMs.toFloat() / totalAudioSpeakingMs.toFloat()
            } else 0.0f

            SpeakerClusterStats(
                cluster = cluster,
                totalSpeakingMs = speakingMs,
                turnCount = clusterTurns.size,
                percentageOfTotal = pct
            )
        }

        SpeakerScreenUiState(
            transcriptId = transcriptId,
            run = run,
            clusters = clusters,
            clusterStats = stats,
            turns = turns,
            assignments = assignments,
            isPro = isPro,
            areModelsReady = areModelsReady,
            downloadState = downloadState,
            selectedSpeakerFilterId = filterId,
            showRenameDialog = renameCluster,
            showMergeDialog = mergeCluster,
            showCountDialog = showCount,
            showSetupDialog = showSetup,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SpeakerScreenUiState(transcriptId = transcriptId)
    )

    fun onAnalyzeClicked() {
        if (!com.example.transcriber.billing.FeatureAccessPolicy.hasAccess(billing.state.value.entitlement, ProFeature.SPEAKER_INTELLIGENCE)) {
            _errorMessage.value = "Speaker Intelligence is a Pro feature"
            return
        }

        if (!modelManager.areModelsReady()) {
            _showSetupDialog.value = true
        } else {
            _showCountDialog.value = true
        }
    }

    fun startDiarization(mode: SpeakerCountMode, requestedCount: Int?) {
        _showCountDialog.value = false
        viewModelScope.launch {
            val transcript = transcriptRepo.getTranscriptById(transcriptId)
            if (transcript == null) {
                _errorMessage.value = "Transcript not found"
                return@launch
            }

            val audioUri = if (!transcript.sourceUri.isNullOrBlank()) {
                Uri.parse(transcript.sourceUri)
            } else if (!transcript.audioUriString.isNullOrBlank()) {
                Uri.parse(transcript.audioUriString)
            } else {
                null
            }

            if (audioUri == null) {
                _errorMessage.value = "Audio source file not found for this transcript"
                return@launch
            }

            SpeakerDiarizationStarter.start(
                context = getApplication(),
                transcriptId = transcriptId,
                audioUri = audioUri,
                countMode = mode,
                requestedSpeakerCount = requestedCount
            )
        }
    }

    fun downloadModels() {
        viewModelScope.launch {
            val result = modelManager.downloadModels()
            if (result.isSuccess) {
                _showSetupDialog.value = false
                _showCountDialog.value = true
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to download models"
            }
        }
    }

    fun dismissSetupDialog() {
        _showSetupDialog.value = false
    }

    fun dismissCountDialog() {
        _showCountDialog.value = false
    }

    fun openRenameDialog(cluster: SpeakerClusterEntity) {
        _renameDialogCluster.value = cluster
    }

    fun dismissRenameDialog() {
        _renameDialogCluster.value = null
    }

    fun confirmRename(clusterId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameCluster(clusterId, newName)
            _renameDialogCluster.value = null
        }
    }

    fun openMergeDialog(cluster: SpeakerClusterEntity) {
        _mergeDialogCluster.value = cluster
    }

    fun dismissMergeDialog() {
        _mergeDialogCluster.value = null
    }

    fun confirmMerge(sourceClusterId: Long, targetClusterId: Long) {
        viewModelScope.launch {
            repository.mergeClusters(sourceClusterId, targetClusterId)
            _mergeDialogCluster.value = null
        }
    }

    fun deleteDiarization() {
        viewModelScope.launch {
            repository.deleteRunAndData(transcriptId)
        }
    }

    fun selectSpeakerFilter(clusterId: Long?) {
        _selectedFilterId.value = if (_selectedFilterId.value == clusterId) null else clusterId
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
