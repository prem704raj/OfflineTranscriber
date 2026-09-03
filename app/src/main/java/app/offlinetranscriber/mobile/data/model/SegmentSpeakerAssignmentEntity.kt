package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "segment_speaker_assignments",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
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
        Index("speakerClusterId")
    ]
)
data class SegmentSpeakerAssignmentEntity(
    @PrimaryKey
    val segmentId: Long,
    val speakerClusterId: Long,
    val overlapRatio: Float
)
