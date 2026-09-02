package com.example.transcriber.ask.model

enum class AskScope {
    TRANSCRIPT,
    LIBRARY
}

enum class AskRole {
    USER,
    ASSISTANT
}

enum class AskEngine {
    CLASSIC,
    GEMINI_NANO
}

data class AskEvidence(
    val segmentId: Long,
    val transcriptId: Long,
    val transcriptTitle: String,
    val mediaType: String,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val rank: Int
)

data class AskCitation(
    val segmentId: Long,
    val transcriptId: Long,
    val transcriptTitle: String,
    val mediaType: String,
    val startMs: Long,
    val quotePreview: String
)

data class GroundedAskAnswer(
    val answer: String,
    val engine: AskEngine,
    val citations: List<AskCitation>,
    val insufficientEvidence: Boolean = false
)
