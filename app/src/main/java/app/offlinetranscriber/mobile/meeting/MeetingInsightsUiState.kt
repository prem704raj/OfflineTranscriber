package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.repository.MeetingPackState
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState

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
