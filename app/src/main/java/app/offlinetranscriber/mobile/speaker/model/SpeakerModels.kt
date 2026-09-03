package app.offlinetranscriber.mobile.speaker.model

enum class SpeakerDiarizationStatus {
    NOT_STARTED,
    PREPARING_AUDIO,
    DIARIZING,
    ALIGNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SpeakerCountMode {
    AUTO,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    CUSTOM
}

data class RawSpeakerTurn(
    val localSpeaker: Int,
    val startMs: Long,
    val endMs: Long
)

data class GlobalSpeakerTurn(
    val globalSpeakerIndex: Int,
    val startMs: Long,
    val endMs: Long
)

data class SpeakerPrototype(
    val globalSpeakerIndex: Int,
    val embedding: FloatArray
)

data class SegmentSpeakerMatch(
    val segmentId: Long,
    val speakerClusterId: Long?,
    val overlapRatio: Float
)
