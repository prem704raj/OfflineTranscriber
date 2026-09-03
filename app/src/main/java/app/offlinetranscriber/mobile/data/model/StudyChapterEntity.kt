package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_chapters",
    foreignKeys = [
        ForeignKey(
            entity = StudyPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["studyPackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studyPackId")]
)
data class StudyChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val studyPackId: Long,
    val position: Int,
    val title: String,
    val startMs: Long,
    val summary: String
)
