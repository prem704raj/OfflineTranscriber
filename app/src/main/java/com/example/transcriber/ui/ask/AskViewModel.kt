package com.example.transcriber.ui.ask

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.ask.AskAnswerCoordinator
import com.example.transcriber.ask.AskQueryNormalizer
import com.example.transcriber.ask.model.AskEngine
import com.example.transcriber.ask.model.AskRole
import com.example.transcriber.ask.model.AskScope
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.data.model.AskMessageEntity
import com.example.transcriber.study.nano.NanoCapabilityManager
import com.example.transcriber.study.nano.NanoFeatureState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AskViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as TranscriberApplication
    private val askRepository = app.askRepository
    private val transcriptRepository = app.transcriptRepository
    private val entitlementRepository = app.entitlementRepository

    private val nanoManager = NanoCapabilityManager()
    private val coordinator = AskAnswerCoordinator(
        retriever = askRepository.retriever(),
        nano = nanoManager
    )

    private val scope: AskScope = runCatching {
        savedStateHandle.get<String>("scope")?.let { AskScope.valueOf(it) }
    }.getOrNull() ?: AskScope.LIBRARY

    private val transcriptId: Long? = savedStateHandle.get<Long>("transcriptId")

    private val _uiState = MutableStateFlow(
        AskUiState(
            scope = scope,
            transcriptId = transcriptId,
            question = savedStateHandle.get<String>("question") ?: ""
        )
    )
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    private var activeConversationJob: Job? = null

    init {
        // Collect entitlement state
        viewModelScope.launch {
            entitlementRepository.entitlement.collect { ent ->
                _uiState.update { it.copy(entitlement = ent) }
            }
        }

        // Initialize nano status
        viewModelScope.launch {
            val initial = nanoManager.refresh()
            _uiState.update { it.copy(nanoStatus = initial) }
            nanoManager.state.collect { state ->
                _uiState.update { it.copy(nanoStatus = state) }
            }
        }

        // Load transcript title if transcript scope
        viewModelScope.launch {
            if (scope == AskScope.TRANSCRIPT && transcriptId != null) {
                val transcript = transcriptRepository.getTranscriptById(transcriptId)
                val title = transcript?.title ?: "Transcript"
                _uiState.update { it.copy(transcriptTitle = title) }
                initConversation(title)
            } else {
                initConversation("Ask Library")
            }
        }
    }

    private fun initConversation(title: String) {
        viewModelScope.launch {
            val convId = askRepository.getOrCreateConversation(
                scope = scope,
                transcriptId = transcriptId,
                title = title
            )
            _uiState.update { it.copy(conversationId = convId) }
            observeMessages(convId)
        }
    }

    private fun observeMessages(conversationId: Long) {
        activeConversationJob?.cancel()
        activeConversationJob = viewModelScope.launch {
            askRepository.observeMessages(conversationId).collect { rawMessages ->
                val uiMessages = rawMessages.map { entity ->
                    val role = runCatching { AskRole.valueOf(entity.role) }.getOrDefault(AskRole.USER)
                    val engine = entity.engine?.let {
                        runCatching { AskEngine.valueOf(it) }.getOrNull()
                    }
                    val citations = if (role == AskRole.ASSISTANT) {
                        askRepository.resolvedCitations(entity.id)
                    } else {
                        emptyList()
                    }
                    AskMessageUiModel(
                        id = entity.id,
                        role = role,
                        text = entity.text,
                        engine = engine,
                        insufficientEvidence = entity.insufficientEvidence,
                        citations = citations,
                        createdAt = entity.createdAt
                    )
                }
                _uiState.update { it.copy(messages = uiMessages) }
            }
        }
    }

    fun onQuestionChange(value: String) {
        val capped = value.take(AskQueryNormalizer.MAX_QUESTION_CHARS)
        savedStateHandle["question"] = capped
        _uiState.update { it.copy(question = capped) }
    }

    fun askQuestion(customText: String? = null, onProRequired: () -> Unit) {
        val raw = customText ?: _uiState.value.question
        val clean = AskQueryNormalizer.clean(raw)
        if (clean.length < 2) return

        if (_uiState.value.entitlement != Entitlement.PRO) {
            onProRequired()
            return
        }

        val convId = _uiState.value.conversationId ?: return
        if (_uiState.value.asking) return

        _uiState.update {
            it.copy(
                asking = true,
                question = if (customText == null) "" else it.question
            )
        }
        savedStateHandle["question"] = _uiState.value.question

        viewModelScope.launch {
            try {
                // 1. Save user question
                askRepository.saveQuestion(convId, clean)

                // 2. Answer question
                val answer = coordinator.ask(
                    question = clean,
                    scope = scope,
                    transcriptId = transcriptId,
                    preferEnhanced = true
                )

                // 3. Save answer + citations
                askRepository.saveAnswer(convId, answer)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = e.message ?: "Failed to answer question.")
                }
            } finally {
                _uiState.update { it.copy(asking = false) }
            }
        }
    }

    fun clearHistory() {
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            askRepository.clearConversation(convId)
            val title = _uiState.value.transcriptTitle ?: "Ask Library"
            initConversation(title)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        super.onCleared()
        nanoManager.close()
    }
}
