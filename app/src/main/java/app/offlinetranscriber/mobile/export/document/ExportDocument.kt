package app.offlinetranscriber.mobile.export.document

data class ExportDocument(
    val title: String,
    val subtitle: String? = null,
    val metadata: List<MetadataItem> = emptyList(),
    val blocks: List<ExportBlock>
)

data class MetadataItem(
    val label: String,
    val value: String
)

sealed interface ExportBlock {

    data class Heading(
        val level: Int,
        val text: String
    ) : ExportBlock

    data class Paragraph(
        val text: String
    ) : ExportBlock

    data class TranscriptSegment(
        val text: String,
        val timestampMs: Long?,
        val speakerLabel: String?
    ) : ExportBlock

    data class Bullet(
        val text: String,
        val timestampMs: Long? = null
    ) : ExportBlock

    data class Checklist(
        val checked: Boolean,
        val text: String,
        val detail: String? = null,
        val timestampMs: Long? = null
    ) : ExportBlock

    data class KeyValue(
        val key: String,
        val value: String
    ) : ExportBlock

    data object Divider : ExportBlock

    data object Spacer : ExportBlock
}
