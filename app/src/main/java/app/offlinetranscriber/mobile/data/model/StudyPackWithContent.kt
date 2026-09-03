package app.offlinetranscriber.mobile.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class StudyPackWithContent(
    @Embedded
    val pack: StudyPackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "studyPackId"
    )
    val keyPoints: List<StudyKeyPointEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "studyPackId"
    )
    val chapters: List<StudyChapterEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "studyPackId"
    )
    val flashcards: List<FlashcardEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "studyPackId"
    )
    val quizQuestions: List<QuizQuestionEntity>
)
