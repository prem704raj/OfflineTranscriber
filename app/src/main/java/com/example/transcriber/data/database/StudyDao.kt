package com.example.transcriber.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.transcriber.data.model.FlashcardEntity
import com.example.transcriber.data.model.QuizQuestionEntity
import com.example.transcriber.data.model.StudyChapterEntity
import com.example.transcriber.data.model.StudyKeyPointEntity
import com.example.transcriber.data.model.StudyPackEntity
import com.example.transcriber.data.model.StudyPackWithContent
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    @Transaction
    @Query(
        "SELECT * FROM study_packs WHERE transcriptId = :transcriptId LIMIT 1"
    )
    fun observeStudyPack(
        transcriptId: Long
    ): Flow<StudyPackWithContent?>

    @Query(
        "SELECT * FROM study_packs WHERE transcriptId = :transcriptId LIMIT 1"
    )
    suspend fun getStudyPack(
        transcriptId: Long
    ): StudyPackEntity?

    @Transaction
    @Query(
        "SELECT * FROM study_packs WHERE transcriptId = :transcriptId LIMIT 1"
    )
    suspend fun getStudyPackWithContent(
        transcriptId: Long
    ): StudyPackWithContent?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPack(
        value: StudyPackEntity
    ): Long

    @Insert
    suspend fun insertKeyPoints(
        values: List<StudyKeyPointEntity>
    )

    @Insert
    suspend fun insertChapters(
        values: List<StudyChapterEntity>
    )

    @Insert
    suspend fun insertFlashcards(
        values: List<FlashcardEntity>
    )

    @Insert
    suspend fun insertQuizQuestions(
        values: List<QuizQuestionEntity>
    )

    @Query(
        "DELETE FROM study_packs WHERE transcriptId = :transcriptId"
    )
    suspend fun deletePackForTranscript(
        transcriptId: Long
    )

    @Query(
        "UPDATE flashcards SET status = :status WHERE id = :flashcardId"
    )
    suspend fun updateFlashcardStatus(
        flashcardId: Long,
        status: String
    )
}
