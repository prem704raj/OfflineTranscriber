package com.example.transcriber.study

import com.example.transcriber.data.model.TranscriptEntity
import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.study.model.StudyDraft
import com.example.transcriber.study.nano.NanoCapabilityManager
import com.example.transcriber.study.nano.NanoFeatureState
import com.example.transcriber.study.nano.NanoStudyEngine

class StudyPackGenerator(
    private val nanoManager: NanoCapabilityManager,
    private val classicEngine: ClassicStudyEngine =
        ClassicStudyEngine()
) {

    suspend fun generate(
        transcript: TranscriptEntity,
        segments: List<TranscriptSegmentEntity>,
        preferEnhanced: Boolean = true
    ): StudyDraft {
        require(segments.isNotEmpty()) {
            "Nothing to study yet."
        }

        if (preferEnhanced) {
            val nanoState = nanoManager.refresh()

            if (nanoState is NanoFeatureState.Available) {
                val enhanced = runCatching {
                    NanoStudyEngine(
                        nanoManager.model()
                    ).generate(
                        transcript = transcript,
                        segments = segments
                    )
                }.getOrNull()

                if (enhanced != null) {
                    return enhanced
                }
            }
        }

        return classicEngine.generate(
            transcript = transcript,
            segments = segments
        )
    }
}
