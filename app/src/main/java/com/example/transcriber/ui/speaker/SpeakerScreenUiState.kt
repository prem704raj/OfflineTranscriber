package com.example.transcriber.ui.speaker

import com.example.transcriber.data.database.SpeakerAssignmentRow
import com.example.transcriber.data.database.SpeakerTurnRow
import com.example.transcriber.data.model.SpeakerClusterEntity
import com.example.transcriber.data.model.SpeakerDiarizationRunEntity
import com.example.transcriber.speaker.modelmanager.SpeakerModelDownloadState

data class SpeakerClusterStats(
    val cluster: SpeakerClusterEntity,
    val totalSpeakingMs: Long,
    val turnCount: Int,
    val percentageOfTotal: Float
)

data class SpeakerScreenUiState(
    val transcriptId: Long = 0L,
    val title: String = "",
    val durationMs: Long = 0L,
    val run: SpeakerDiarizationRunEntity? = null,
    val clusters: List<SpeakerClusterEntity> = emptyList(),
    val clusterStats: List<SpeakerClusterStats> = emptyList(),
    val turns: List<SpeakerTurnRow> = emptyList(),
    val assignments: List<SpeakerAssignmentRow> = emptyList(),
    val isPro: Boolean = false,
    val areModelsReady: Boolean = false,
    val downloadState: SpeakerModelDownloadState = SpeakerModelDownloadState.Idle,
    val selectedSpeakerFilterId: Long? = null,
    val showRenameDialog: SpeakerClusterEntity? = null,
    val showMergeDialog: SpeakerClusterEntity? = null,
    val showCountDialog: Boolean = false,
    val showSetupDialog: Boolean = false,
    val errorMessage: String? = null
)
