package app.offlinetranscriber.mobile.data.model

data class AskCitationRow(
    val messageId: Long,
    val segmentId: Long,
    val transcriptId: Long,
    val transcriptTitle: String,
    val mediaType: String,
    val startMs: Long,
    val text: String,
    val position: Int
)
