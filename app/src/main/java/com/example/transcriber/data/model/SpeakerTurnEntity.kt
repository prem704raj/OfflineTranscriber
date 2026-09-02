package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speaker_turns",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SpeakerClusterEntity::class,
            parentColumns = ["id"],
            childColumns = ["speakerClusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transcriptId"),
        Index("speakerClusterId"),
        Index("startMs")
    ]
)
data class SpeakerTurnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transcriptId: Long,
    val speakerClusterId: Long,
    val startMs: Long,
    val endMs: Long
)
