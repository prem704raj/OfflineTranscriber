package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meeting_topics",
    foreignKeys = [
        ForeignKey(
            entity = MeetingPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingPackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("meetingPackId"),
        Index("startMs")
    ]
)
data class MeetingTopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val meetingPackId: Long,
    val title: String,
    val sourceSegmentId: Long?,
    val startMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
