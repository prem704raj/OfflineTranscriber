package com.example.transcriber.export.files

import com.example.transcriber.export.model.ExportFormat

object ExportFileName {

    private val invalid = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    fun create(
        title: String,
        format: ExportFormat,
        suffix: String? = null
    ): String {
        val base = title
            .replace(invalid, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('.')
            .take(80)
            .ifBlank { "Offline Transcriber" }

        val safeSuffix = suffix
            ?.replace(invalid, " ")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.take(30)
            ?.takeIf { it.isNotBlank() }

        return buildString {
            append(base)
            if (safeSuffix != null) {
                append(" - ")
                append(safeSuffix)
            }
            append(".")
            append(format.extension)
        }
    }
}
