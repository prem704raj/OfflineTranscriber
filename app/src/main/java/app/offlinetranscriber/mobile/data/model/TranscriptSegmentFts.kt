package app.offlinetranscriber.mobile.data.model

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

@Fts4(
    contentEntity = TranscriptSegmentEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(tableName = "transcript_segments_fts")
data class TranscriptSegmentFts(
    val text: String
)
