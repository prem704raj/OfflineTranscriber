package app.offlinetranscriber.mobile.data.model

data class AskEvidenceRow(
    val segmentId: Long,
    val transcriptId: Long,
    val transcriptTitle: String,
    val mediaType: String,
    val startMs: Long,
    val endMs: Long,
    val text: String
)
