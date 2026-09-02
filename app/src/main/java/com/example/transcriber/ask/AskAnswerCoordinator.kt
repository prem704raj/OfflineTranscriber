package com.example.transcriber.ask

import com.example.transcriber.ask.model.AskScope
import com.example.transcriber.ask.model.GroundedAskAnswer
import com.example.transcriber.study.nano.NanoCapabilityManager
import com.example.transcriber.study.nano.NanoFeatureState

class AskAnswerCoordinator(
    private val retriever: AskEvidenceRetriever,
    private val nano: NanoCapabilityManager,
    private val classic: ClassicAskEngine = ClassicAskEngine()
) {

    suspend fun ask(
        question: String,
        scope: AskScope,
        transcriptId: Long?,
        preferEnhanced: Boolean = true
    ): GroundedAskAnswer {

        val evidence = retriever.retrieve(
            question,
            scope,
            transcriptId
        )

        if (evidence.isEmpty()) {
            return classic.answer(
                question,
                evidence
            )
        }

        if (preferEnhanced) {
            val status = runCatching {
                nano.refresh()
            }.getOrNull()

            if (status is NanoFeatureState.Available) {
                val result = runCatching {
                    NanoAskEngine(
                        nano.model()
                    ).answer(
                        question,
                        evidence
                    )
                }.getOrNull()

                if (result != null) {
                    return result
                }
            }
        }

        return classic.answer(
            question,
            evidence
        )
    }
}
