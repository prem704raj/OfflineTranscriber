package app.offlinetranscriber.mobile.study

import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.study.model.StudyDraft
import app.offlinetranscriber.mobile.study.nano.NanoCapabilityManager
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState
import app.offlinetranscriber.mobile.study.nano.NanoStudyEngine

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
