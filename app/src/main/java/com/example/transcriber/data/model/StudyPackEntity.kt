package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_packs",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcriptId"], unique = true)
    ]
)
data class StudyPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transcriptId: Long,
    val engine: String,
    val generatedAt: Long = System.currentTimeMillis()
)
