package com.example.transcriber.export.render

import com.example.transcriber.export.document.ExportBlock
import com.example.transcriber.export.document.ExportDocument
import com.example.transcriber.export.format.ExportTimestampFormatter
import com.example.transcriber.export.model.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class MarkdownExportRenderer : ExportRenderer {

    override suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long = withContext(Dispatchers.IO) {
        FileOutputStream(targetFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Title & Subtitle
                writer.write("# ${document.title}\n\n")

                if (document.subtitle != null) {
                    writer.write("*${document.subtitle}*\n\n")
                }

                // Metadata
                if (document.metadata.isNotEmpty()) {
                    writer.write("### Overview\n\n")
                    document.metadata.forEach { item ->
                        writer.write("- **${item.label}:** ${item.value}\n")
                    }
                    writer.write("\n---\n\n")
                }

                // Blocks
                document.blocks.forEach { block ->
                    when (block) {
                        is ExportBlock.Heading -> {
                            val hashes = "#".repeat((block.level + 1).coerceIn(2, 5))
                            writer.write("\n$hashes ${block.text}\n\n")
                        }
                        is ExportBlock.Paragraph -> {
                            writer.write("${block.text}\n\n")
                        }
                        is ExportBlock.TranscriptSegment -> {
                            val prefix = buildString {
                                if (block.timestampMs != null) {
                                    append("**`[${ExportTimestampFormatter.format(block.timestampMs)}]`** ")
                                }
                                if (block.speakerLabel != null) {
                                    append("**${block.speakerLabel}:** ")
                                }
                            }
                            writer.write("$prefix${block.text}\n\n")
                        }
                        is ExportBlock.Bullet -> {
                            val time = if (block.timestampMs != null) "`[${ExportTimestampFormatter.format(block.timestampMs)}]` " else ""
                            writer.write("- $time${block.text}\n")
                        }
                        is ExportBlock.Checklist -> {
                            val mark = if (block.checked) "- [x]" else "- [ ]"
                            val time = if (block.timestampMs != null) " `[${ExportTimestampFormatter.format(block.timestampMs)}]`" else ""
                            val detail = if (block.detail != null) " *(${block.detail})*" else ""
                            writer.write("$mark ${block.text}$detail$time\n")
                        }
                        is ExportBlock.KeyValue -> {
                            writer.write("**${block.key}:** ${block.value}\n\n")
                        }
                        is ExportBlock.Divider -> {
                            writer.write("\n---\n\n")
                        }
                        is ExportBlock.Spacer -> {
                            writer.write("\n")
                        }
                    }
                }
                writer.flush()
            }
        }
        targetFile.length()
    }
}
