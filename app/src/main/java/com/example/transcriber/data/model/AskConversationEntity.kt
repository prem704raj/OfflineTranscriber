package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ask_conversations",
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
        Index("updatedAt")
    ]
)
data class AskConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scope: String,
    val transcriptId: Long?,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
