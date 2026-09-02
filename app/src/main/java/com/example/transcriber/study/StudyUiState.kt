package com.example.transcriber.study

import com.example.transcriber.data.model.StudyPackWithContent
import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.study.nano.NanoFeatureState

enum class StudyTab {
    OVERVIEW,
    CHAPTERS,
    FLASHCARDS,
    QUIZ
}

data class QuizAttemptState(
    val questionIndex: Int = 0,
    val selectedIndex: Int? = null,
    val answered: Boolean = false,
    val score: Int = 0,
    val finished: Boolean = false
)

data class StudyUiState(
    val transcript: TranscriptEntity? = null,
    val pack: StudyPackWithContent? = null,
    val nanoState: NanoFeatureState =
        NanoFeatureState.Checking,
    val generating: Boolean = false,
    val activeTab: StudyTab = StudyTab.OVERVIEW,
    val flashcardIndex: Int = 0,
    val flashcardShowingBack: Boolean = false,
    val flashcardOrder: List<Long> = emptyList(),
    val quiz: QuizAttemptState = QuizAttemptState(),
    val message: String? = null
)
