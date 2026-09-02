package com.example.transcriber.study

import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.model.StudyDraft

interface StudyEngine {
    suspend fun generate(
        transcript: TranscriptEntity,
        segments: List<TranscriptSegmentEntity>
    ): StudyDraft
}
