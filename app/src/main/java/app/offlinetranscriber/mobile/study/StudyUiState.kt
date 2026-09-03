package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.StudyPackWithContent
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState

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
