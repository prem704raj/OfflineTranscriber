package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ask_messages",
    foreignKeys = [
        ForeignKey(
            entity = AskConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId"),
        Index("createdAt")
    ]
)
data class AskMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val conversationId: Long,
    val role: String,
    val text: String,
    val engine: String?,
    val insufficientEvidence: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
