package com.example.transcriber.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transcript_collection_cross_ref",
    primaryKeys = ["transcriptId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"])
    ]
)
data class TranscriptCollectionCrossRef(
    val transcriptId: Long,
    val collectionId: Long
)
