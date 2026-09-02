package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["studyPackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studyPackId")]
)
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studyPackId: Long,
    val position: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String
) {
    fun options(): List<String> = listOf(
        optionA,
        optionB,
        optionC,
        optionD
    )
}
