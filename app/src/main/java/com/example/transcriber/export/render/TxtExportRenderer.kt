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

class TxtExportRenderer : ExportRenderer {

    override suspend fun render(
        document: ExportDocument,
        options: ExportOptions,
        targetFile: File
    ): Long = withContext(Dispatchers.IO) {
        FileOutputStream(targetFile).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Title & Subtitle
                writer.write(document.title.uppercase())
                writer.write("\n")
                writer.write("=".repeat(document.title.length.coerceIn(10, 60)))
                writer.write("\n")

                if (document.subtitle != null) {
                    writer.write(document.subtitle)
                    writer.write("\n")
                }

                // Metadata
                if (document.metadata.isNotEmpty()) {
                    writer.write("\n")
                    document.metadata.forEach { item ->
                        writer.write("${item.label}: ${item.value}\n")
                    }
                    writer.write("-".repeat(40))
                    writer.write("\n\n")
                } else {
                    writer.write("\n")
                }

                // Blocks
                document.blocks.forEach { block ->
                    when (block) {
                        is ExportBlock.Heading -> {
                            writer.write("\n")
                            if (block.level == 1) {
                                writer.write(block.text.uppercase())
                                writer.write("\n")
                                writer.write("-".repeat(block.text.length.coerceIn(5, 40)))
                                writer.write("\n")
                            } else {
                                writer.write("## ${block.text}\n")
                            }
                        }
                        is ExportBlock.Paragraph -> {
                            writer.write("${block.text}\n\n")
                        }
                        is ExportBlock.TranscriptSegment -> {
                            val prefix = buildString {
                                if (block.timestampMs != null) {
                                    append("[${ExportTimestampFormatter.format(block.timestampMs)}] ")
                                }
                                if (block.speakerLabel != null) {
                                    append("[${block.speakerLabel}] ")
                                }
                            }
                            writer.write("$prefix${block.text}\n\n")
                        }
                        is ExportBlock.Bullet -> {
                            val time = if (block.timestampMs != null) "[${ExportTimestampFormatter.format(block.timestampMs)}] " else ""
                            writer.write("• $time${block.text}\n")
                        }
                        is ExportBlock.Checklist -> {
                            val mark = if (block.checked) "[X]" else "[ ]"
                            val time = if (block.timestampMs != null) " [${ExportTimestampFormatter.format(block.timestampMs)}]" else ""
                            val detail = if (block.detail != null) " (${block.detail})" else ""
                            writer.write("$mark ${block.text}$detail$time\n")
                        }
                        is ExportBlock.KeyValue -> {
                            writer.write("${block.key}: ${block.value}\n")
                        }
                        is ExportBlock.Divider -> {
                            writer.write("\n----------------------------------------\n\n")
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
