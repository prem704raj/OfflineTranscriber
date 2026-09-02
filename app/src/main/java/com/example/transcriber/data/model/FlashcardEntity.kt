package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
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
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studyPackId: Long,
    val position: Int,
    val front: String,
    val back: String,
    val status: String = "NEW"
)
