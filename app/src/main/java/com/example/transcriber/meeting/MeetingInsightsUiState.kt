package com.example.transcriber.meeting

import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.repository.MeetingPackState
import com.example.transcriber.study.nano.NanoFeatureState

data class MeetingInsightsUiState(
    val transcript: TranscriptEntity? = null,
    val pack: MeetingPackState? = null,
    val selectedTab: MeetingInsightsTab = MeetingInsightsTab.OVERVIEW,
    val generating: Boolean = false,
    val progress: Int = 0,
    val foreground: Boolean = false,
    val nanoState: NanoFeatureState = NanoFeatureState.Checking,
    val message: String? = null
)
