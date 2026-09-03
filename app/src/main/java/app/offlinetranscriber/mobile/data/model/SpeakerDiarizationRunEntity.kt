package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "speaker_diarization_runs",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["transcriptId"],
            unique = true
        ),
        Index("status"),
        Index("updatedAt")
    ]
)
data class SpeakerDiarizationRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val transcriptId: Long,
    val status: String,
    val progress: Int = 0,
    val speakerCountMode: String = "AUTO",
    val requestedSpeakerCount: Int? = null,
    val detectedSpeakerCount: Int = 0,
    val engineVersion: String,
    val segmentationModelId: String,
    val embeddingModelId: String,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
