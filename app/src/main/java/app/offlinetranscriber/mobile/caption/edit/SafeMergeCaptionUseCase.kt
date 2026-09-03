package app.offlinetranscriber.mobile.caption.edit

import androidx.room.withTransaction
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SafeMergeCaptionUseCase(
    private val database: AppDatabase
) {

    suspend fun merge(
        transcriptId: Long,
        currentSegmentId: Long,
        nextSegmentId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val transcriptDao = database.transcriptDao()

                val allSegments = transcriptDao.getSegmentsOnce(transcriptId)
                val current = allSegments.find { it.id == currentSegmentId }
                    ?: error("Current segment not found.")
                val next = allSegments.find { it.id == nextSegmentId }
                    ?: error("Next segment not found.")

                val mergedText = "${current.text.trim()} ${next.text.trim()}"
                val mergedStart = current.startMs
                val mergedEnd = maxOf(current.endMs, next.endMs)

                // 1. Re-point dependent references from next to current before deletion
                val db = database.openHelper.writableDatabase
                db.execSQL("UPDATE bookmarks SET segmentId = ? WHERE segmentId = ?", arrayOf(current.id, next.id))
                try {
                    db.execSQL("UPDATE ask_citations SET segmentId = ? WHERE segmentId = ?", arrayOf(current.id, next.id))
                } catch (_: Exception) {}
                try {
                    db.execSQL("UPDATE meeting_topics SET sourceSegmentId = ? WHERE sourceSegmentId = ?", arrayOf(current.id, next.id))
                    db.execSQL("UPDATE meeting_decisions SET sourceSegmentId = ? WHERE sourceSegmentId = ?", arrayOf(current.id, next.id))
                    db.execSQL("UPDATE meeting_questions SET sourceSegmentId = ? WHERE sourceSegmentId = ?", arrayOf(current.id, next.id))
                    db.execSQL("UPDATE meeting_action_items SET sourceSegmentId = ? WHERE sourceSegmentId = ?", arrayOf(current.id, next.id))
                } catch (_: Exception) {}

                // 2. Update current segment
                transcriptDao.updateSegmentEntity(
                    segmentId = current.id,
                    text = mergedText,
                    startMs = mergedStart,
                    endMs = mergedEnd
                )

                // 3. Delete next segment
                db.execSQL("DELETE FROM transcript_segments WHERE id = ?", arrayOf(next.id))

                // 4. Update transcript JSON and full text
                val refreshedSegments = transcriptDao.getSegmentsOnce(transcriptId)
                val domainSegments = refreshedSegments.map { seg ->
                    TranscriptSegment(
                        id = seg.id,
                        startMs = seg.startMs,
                        endMs = seg.endMs,
                        text = seg.text
                    )
                }
                val updatedJson = Json.encodeToString(domainSegments)
                val updatedFullText = domainSegments.joinToString(" ") { it.text.trim() }

                transcriptDao.updateSegments(transcriptId, updatedJson, updatedFullText)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
