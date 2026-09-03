package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meeting_actions",
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
        Index("status"),
        Index("startMs")
    ]
)
data class MeetingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val meetingPackId: Long,
    val text: String,
    val assignee: String = "",
    val dueText: String = "",
    val sourceSegmentId: Long?,
    val startMs: Long,
    val status: String = "OPEN",
    val manuallyEdited: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
