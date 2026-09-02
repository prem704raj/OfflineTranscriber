package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ask_citations",
    foreignKeys = [
        ForeignKey(
            entity = AskMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TranscriptSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("messageId"),
        Index("segmentId")
    ]
)
data class AskCitationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val messageId: Long,
    val segmentId: Long,
    val position: Int
)
