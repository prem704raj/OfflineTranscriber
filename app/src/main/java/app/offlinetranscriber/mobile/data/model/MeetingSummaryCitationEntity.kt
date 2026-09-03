package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "meeting_summary_citations",
    primaryKeys = ["meetingPackId", "segmentId"],
    foreignKeys = [
        ForeignKey(
            entity = MeetingPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingPackId"],
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
        Index("meetingPackId"),
        Index("segmentId")
    ]
)
data class MeetingSummaryCitationEntity(
    val meetingPackId: Long,
    val segmentId: Long,
    val position: Int
)
