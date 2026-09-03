package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.study.model.StudyDraft

interface StudyEngine {
    suspend fun generate(
        transcript: TranscriptEntity,
        segments: List<TranscriptSegmentEntity>
    ): StudyDraft
}
