package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_key_points",
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
data class StudyKeyPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studyPackId: Long,
    val position: Int,
    val text: String
)
