package com.example.transcriber.data.repository

import androidx.room.withTransaction
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.data.model.FlashcardEntity
import com.example.transcriber.data.model.QuizQuestionEntity
import com.example.transcriber.data.model.StudyChapterEntity
import com.example.transcriber.data.model.StudyKeyPointEntity
import com.example.transcriber.data.model.StudyPackEntity
import com.example.transcriber.study.model.FlashcardStatus
import com.example.transcriber.study.model.StudyDraft

class StudyRepository(
    private val database: AppDatabase
) {

    private val dao = database.studyDao()

    fun observePack(transcriptId: Long) =
        dao.observeStudyPack(transcriptId)

    suspend fun replacePack(
        transcriptId: Long,
        draft: StudyDraft
    ) {
        database.withTransaction {
            dao.deletePackForTranscript(transcriptId)

            val packId = dao.insertPack(
                StudyPackEntity(
                    transcriptId = transcriptId,
                    engine = draft.engine.name
                )
            )

            dao.insertKeyPoints(
                draft.keyPoints
                    .mapIndexed { index, value ->
                        StudyKeyPointEntity(
                            studyPackId = packId,
                            position = index,
                            text = value
                        )
                    }
            )

            dao.insertChapters(
                draft.chapters
                    .mapIndexed { index, value ->
                        StudyChapterEntity(
                            studyPackId = packId,
                            position = index,
                            title = value.title,
                            startMs = value.startMs,
                            summary = value.summary
                        )
                    }
            )

            dao.insertFlashcards(
                draft.flashcards
                    .mapIndexed { index, value ->
                        FlashcardEntity(
                            studyPackId = packId,
                            position = index,
                            front = value.front,
                            back = value.back
                        )
                    }
            )

            dao.insertQuizQuestions(
                draft.quizQuestions
                    .filter { it.options.size == 4 }
                    .mapIndexed { index, value ->
                        QuizQuestionEntity(
                            studyPackId = packId,
                            position = index,
                            question = value.question,
                            optionA = value.options[0],
                            optionB = value.options[1],
                            optionC = value.options[2],
                            optionD = value.options[3],
                            correctIndex =
                                value.correctIndex.coerceIn(0, 3),
                            explanation = value.explanation
                        )
                    }
            )
        }
    }

    suspend fun deletePack(
        transcriptId: Long
    ) {
        dao.deletePackForTranscript(transcriptId)
    }

    suspend fun setFlashcardStatus(
        flashcardId: Long,
        status: FlashcardStatus
    ) {
        dao.updateFlashcardStatus(
            flashcardId = flashcardId,
            status = status.name
        )
    }
}
