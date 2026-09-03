package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val audioFileName: String,
    val audioDurationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val fullText: String,
    val segmentsJson: String,
    val modelUsed: String,
    val audioUriString: String? = null,
    val sourceUri: String = audioUriString ?: "",
    val mediaType: String = MediaType.AUDIO
) {
    fun getSegments(): List<TranscriptSegment> {
        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val decoded: List<TranscriptSegment> = json.decodeFromString(segmentsJson)
            decoded.mapIndexed { index, seg ->
                if (seg.id == 0L) seg.copy(id = (index + 1).toLong()) else seg
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun fromSegments(
            title: String,
            audioFileName: String,
            audioDurationMs: Long,
            segments: List<TranscriptSegment>,
            modelUsed: String,
            audioUriString: String? = null,
            sourceUri: String = audioUriString ?: "",
            mediaType: String = MediaType.AUDIO
        ): TranscriptEntity {
            val numberedSegments = segments.mapIndexed { index, seg ->
                if (seg.id == 0L) seg.copy(id = (index + 1).toLong()) else seg
            }
            val fullText = numberedSegments.joinToString(" ") { it.text.trim() }
            val segmentsJson = Json.encodeToString(numberedSegments)
            return TranscriptEntity(
                title = title,
                audioFileName = audioFileName,
                audioDurationMs = audioDurationMs,
                fullText = fullText,
                segmentsJson = segmentsJson,
                modelUsed = modelUsed,
                audioUriString = audioUriString,
                sourceUri = sourceUri.ifBlank { audioUriString ?: "" },
                mediaType = mediaType
            )
        }
    }
}
