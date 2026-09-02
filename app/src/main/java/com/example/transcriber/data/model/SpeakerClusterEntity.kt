package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speaker_clusters",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transcriptId"),
        Index(
            value = ["transcriptId", "speakerIndex"],
            unique = true
        )
    ]
)
data class SpeakerClusterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transcriptId: Long,
    val speakerIndex: Int,
    val customName: String = "",
    val userNamed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
