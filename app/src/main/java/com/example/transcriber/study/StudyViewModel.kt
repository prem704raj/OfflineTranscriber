package com.example.transcriber.study

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.study.model.FlashcardStatus
import com.example.transcriber.study.nano.NanoCapabilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class StudyViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val transcriptId: Long =
        checkNotNull(savedStateHandle["transcriptId"])

    private val app =
        application as TranscriberApplication

    private val transcriptRepository =
        app.transcriptRepository

    private val studyRepository =
        app.studyRepository

    private val nanoManager =
        NanoCapabilityManager()

    private val generator =
        StudyPackGenerator(nanoManager)

    private val _state =
        MutableStateFlow(StudyUiState())

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transcriptRepository.observeTranscript(
                    transcriptId
                ),
                studyRepository.observePack(
                    transcriptId
                ),
                nanoManager.state
            ) { transcript, pack, nano ->
                Triple(transcript, pack, nano)
            }.collect { (transcript, pack, nano) ->
                val old = _state.value

                val order =
                    if (
                        old.flashcardOrder.isEmpty() &&
                        pack != null
                    ) {
                        pack.flashcards
                            .sortedBy { it.position }
                            .map { it.id }
                    } else {
                        old.flashcardOrder
                    }

                _state.value = old.copy(
                    transcript = transcript,
                    pack = pack,
                    nanoState = nano,
                    flashcardOrder = order
                )
            }
        }

        viewModelScope.launch {
            nanoManager.refresh()
        }
    }

    fun setTab(tab: StudyTab) {
        _state.value = _state.value.copy(
            activeTab = tab
        )
    }

    fun generate(
        preferEnhanced: Boolean = true
    ) {
        if (_state.value.generating) return

        viewModelScope.launch {
            val entitlement = app.entitlementRepository.entitlement.first()
            if (entitlement != com.example.transcriber.billing.Entitlement.PRO) {
                _state.value = _state.value.copy(
                    message = "Study Mode generation requires Offline Transcriber Pro."
                )
                return@launch
            }

            val transcript =
                _state.value.transcript

            if (transcript == null) {
                _state.value = _state.value.copy(
                    message = "Transcript is not available."
                )
                return@launch
            }

            val segments =
                transcriptRepository
                    .getSegmentsOnce(transcriptId)

            if (segments.isEmpty()) {
                _state.value = _state.value.copy(
                    message = "This transcript has no text yet."
                )
                return@launch
            }

            _state.value = _state.value.copy(
                generating = true,
                message = null
            )

            runCatching {
                generator.generate(
                    transcript = transcript,
                    segments = segments,
                    preferEnhanced = preferEnhanced
                )
            }.onSuccess { draft ->
                studyRepository.replacePack(
                    transcriptId = transcriptId,
                    draft = draft
                )

                _state.value = _state.value.copy(
                    generating = false,
                    flashcardIndex = 0,
                    flashcardShowingBack = false,
                    flashcardOrder = emptyList(),
                    quiz = QuizAttemptState()
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    generating = false,
                    message = error.message
                        ?: "Unable to generate study pack."
                )
            }
        }
    }

    fun downloadNano() {
        viewModelScope.launch {
            nanoManager.download()
        }
    }

    fun deletePack() {
        viewModelScope.launch {
            studyRepository.deletePack(transcriptId)
            _state.value = _state.value.copy(
                flashcardOrder = emptyList(),
                quiz = QuizAttemptState()
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(
            message = null
        )
    }

    fun flipFlashcard() {
        _state.value = _state.value.copy(
            flashcardShowingBack =
                !_state.value.flashcardShowingBack
        )
    }

    fun nextFlashcard() {
        val count =
            _state.value.flashcardOrder.size

        if (count == 0) return

        _state.value = _state.value.copy(
            flashcardIndex =
                (_state.value.flashcardIndex + 1)
                    .coerceAtMost(count - 1),
            flashcardShowingBack = false
        )
    }

    fun previousFlashcard() {
        _state.value = _state.value.copy(
            flashcardIndex =
                (_state.value.flashcardIndex - 1)
                    .coerceAtLeast(0),
            flashcardShowingBack = false
        )
    }

    fun shuffleFlashcards() {
        val shuffled =
            _state.value.flashcardOrder
                .shuffled(Random(System.nanoTime()))

        _state.value = _state.value.copy(
            flashcardOrder = shuffled,
            flashcardIndex = 0,
            flashcardShowingBack = false
        )
    }

    fun markCurrentCard(
        status: FlashcardStatus
    ) {
        val cardId =
            _state.value.flashcardOrder
                .getOrNull(_state.value.flashcardIndex)
                ?: return

        viewModelScope.launch {
            studyRepository.setFlashcardStatus(
                cardId,
                status
            )
            nextFlashcard()
        }
    }

    fun answerQuiz(optionIndex: Int) {
        val pack =
            _state.value.pack ?: return

        val questions =
            pack.quizQuestions.sortedBy { it.position }

        val attempt = _state.value.quiz

        if (
            attempt.finished ||
            attempt.answered
        ) return

        val question =
            questions.getOrNull(
                attempt.questionIndex
            ) ?: return

        val correct =
            optionIndex == question.correctIndex

        _state.value = _state.value.copy(
            quiz = attempt.copy(
                selectedIndex = optionIndex,
                answered = true,
                score =
                    attempt.score +
                        if (correct) 1 else 0
            )
        )
    }

    fun nextQuizQuestion() {
        val questions =
            _state.value.pack
                ?.quizQuestions
                ?.sortedBy { it.position }
                .orEmpty()

        val attempt = _state.value.quiz

        if (!attempt.answered) return

        val next = attempt.questionIndex + 1

        _state.value = _state.value.copy(
            quiz = if (next >= questions.size) {
                attempt.copy(
                    finished = true
                )
            } else {
                attempt.copy(
                    questionIndex = next,
                    selectedIndex = null,
                    answered = false
                )
            }
        )
    }

    fun retryQuiz() {
        _state.value = _state.value.copy(
            quiz = QuizAttemptState()
        )
    }

    override fun onCleared() {
        nanoManager.close()
        super.onCleared()
    }
}
