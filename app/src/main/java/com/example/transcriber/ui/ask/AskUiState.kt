package com.example.transcriber.ui.ask

import com.example.transcriber.ask.model.AskEngine
import com.example.transcriber.ask.model.AskRole
import com.example.transcriber.ask.model.AskScope
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.data.model.AskCitationRow
import com.example.transcriber.study.nano.NanoFeatureState

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
