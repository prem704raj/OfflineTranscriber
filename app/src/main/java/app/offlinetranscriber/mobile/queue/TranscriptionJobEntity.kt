package app.offlinetranscriber.mobile.queue

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcription_jobs",
    indices = [
        Index("status"),
        Index("createdAt")
    ]
)
data class TranscriptionJobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * Initial input.
     * AUDIO/RECORDING: actual audio.
     * New Phase-9 VIDEO: raw original video.
     */
    val inputUri: String,

    /**
     * Media the transcript should link back to.
     * VIDEO: original video.
     * AUDIO/RECORDING: original audio.
     */
    val sourceUri: String,

    /**
     * Actual audio fed into Whisper.
     * Null only while a new raw video still needs Media3 extraction.
     */
    val preparedInputUri: String?,

    val sourceType: String,
    val displayName: String,
    val modelId: String,
    val languageCode: String,

    val stage: String = TranscriptionJobStage.TRANSCRIBING.name,

    val status: String = TranscriptionJobStatus.QUEUED.name,

    val progress: Int = 0,

    val resultTranscriptId: Long? = null,
    val errorMessage: String? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis(),

    val preparedPcmPath: String? = null,
    val checkpointSample: Long = 0L,
    val partialSegmentsJson: String = "[]"
)
