package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcriptId"])
    ]
)
data class TranscriptSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transcriptId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String
)
