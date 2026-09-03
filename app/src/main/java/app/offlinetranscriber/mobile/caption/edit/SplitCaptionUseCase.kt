package app.offlinetranscriber.mobile.caption.edit

import androidx.room.withTransaction
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.model.SegmentSpeakerAssignmentEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SplitCaptionUseCase(
    private val database: AppDatabase
) {

    suspend fun split(
        transcriptId: Long,
        segmentId: Long,
        splitIndex: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val transcriptDao = database.transcriptDao()
                val speakerDao = database.speakerDiarizationDao()

                val transcript = transcriptDao.getTranscriptById(transcriptId)
                    ?: error("Transcript not found.")

                val allSegments = transcriptDao.getSegmentsOnce(transcriptId)
                val original = allSegments.find { it.id == segmentId }
                    ?: error("Caption segment not found.")

                val cleanText = original.text
                val safeSplitIndex = splitIndex.coerceIn(1, cleanText.length - 1)

                val left = cleanText.substring(0, safeSplitIndex).trim()
                val right = cleanText.substring(safeSplitIndex).trim()

                require(left.isNotBlank() && right.isNotBlank()) {
                    "Split must produce non-empty text on both sides."
                }

                val totalDuration = original.endMs - original.startMs
                require(totalDuration >= 600L) {
                    "Segment must be at least 600ms long to split."
                }

                val ratio = left.length.toFloat() / (left.length + right.length).toFloat()
                val splitMs = (original.startMs + (totalDuration * ratio)).toLong().coerceIn(
                    original.startMs + 300L,
                    original.endMs - 300L
                )

                // 1. Update first half
                transcriptDao.updateSegmentEntity(
                    segmentId = original.id,
                    text = left,
                    startMs = original.startMs,
                    endMs = splitMs
                )

                // 2. Insert second half
                val secondEntity = TranscriptSegmentEntity(
                    id = 0L,
                    transcriptId = transcriptId,
                    startMs = splitMs,
                    endMs = original.endMs,
                    text = right
                )
                val insertedIds = transcriptDao.insertSegments(listOf(secondEntity))
                val secondId = insertedIds.firstOrNull() ?: 0L

                // 3. Duplicate speaker assignment if present
                if (secondId > 0L) {
                    val currentAssignments = speakerDao.getAssignmentsOnce(transcriptId)
                    val originalAssignment = currentAssignments.find { it.segmentId == original.id }
                    if (originalAssignment != null) {
                        speakerDao.insertAssignments(
                            listOf(
                                SegmentSpeakerAssignmentEntity(
                                    segmentId = secondId,
                                    speakerClusterId = originalAssignment.speakerClusterId,
                                    overlapRatio = originalAssignment.overlapRatio
                                )
                            )
                        )
                    }
                }

                // 4. Update TranscriptEntity segmentsJson and fullText
                val refreshedSegments = transcriptDao.getSegmentsOnce(transcriptId)
                val domainSegments = refreshedSegments.mapIndexed { index, seg ->
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
