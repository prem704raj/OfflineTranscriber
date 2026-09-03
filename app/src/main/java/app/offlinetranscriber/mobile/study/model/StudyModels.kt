package app.offlinetranscriber.mobile.study.model

enum class StudyEngineType {
    CLASSIC,
    GEMINI_NANO
}

enum class FlashcardStatus {
    NEW,
    KNOWN,
    REVIEW
}

data class DraftChapter(
    val title: String,
    val startMs: Long,
    val summary: String
)

data class DraftFlashcard(
    val front: String,
    val back: String
)

data class DraftQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class StudyDraft(
    val engine: StudyEngineType,
    val keyPoints: List<String>,
    val chapters: List<DraftChapter>,
    val flashcards: List<DraftFlashcard>,
    val quizQuestions: List<DraftQuizQuestion>
)
