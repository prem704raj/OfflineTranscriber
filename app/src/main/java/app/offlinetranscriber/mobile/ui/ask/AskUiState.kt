package app.offlinetranscriber.mobile.ui.ask

import app.offlinetranscriber.mobile.ask.model.AskEngine
import app.offlinetranscriber.mobile.ask.model.AskRole
import app.offlinetranscriber.mobile.ask.model.AskScope
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.data.model.AskCitationRow
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState

data class AskMessageUiModel(
    val id: Long,
    val role: AskRole,
    val text: String,
    val engine: AskEngine?,
    val insufficientEvidence: Boolean,
    val citations: List<AskCitationRow>,
    val createdAt: Long
)

data class AskUiState(
    val scope: AskScope = AskScope.LIBRARY,
    val transcriptId: Long? = null,
    val transcriptTitle: String? = null,
    val conversationId: Long? = null,
    val messages: List<AskMessageUiModel> = emptyList(),
    val question: String = "",
    val asking: Boolean = false,
    val nanoStatus: NanoFeatureState = NanoFeatureState.Checking,
    val entitlement: Entitlement = Entitlement.FREE,
    val message: String? = null
)
