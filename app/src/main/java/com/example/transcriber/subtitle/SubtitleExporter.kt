package com.example.transcriber.subtitle

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.transcriber.domain.model.TranscriptSegment
import java.io.File

class SubtitleExporter(private val context: Context) {
    fun writeToUri(
        destination: Uri,
        format: SubtitleFormat,
        segments: List<TranscriptSegment>
    ) {
        val content = when (format) {
            SubtitleFormat.SRT -> SubtitleFormatter.toSrt(segments)
            SubtitleFormat.VTT -> SubtitleFormatter.toVtt(segments)
        }

        context.contentResolver.openOutputStream(destination, "wt")
            ?.bufferedWriter(Charsets.UTF_8)
            ?.use { it.write(content) }
            ?: error("Unable to open subtitle destination.")
    }

    fun createShareSrtIntent(
        title: String,
        segments: List<TranscriptSegment>
    ): Intent {
        val dir = File(context.cacheDir, "shared_subtitles").apply { mkdirs() }
        val safe = title.replace(Regex("[^a-zA-Z0-9._-]+"), "_")
            .trim('_')
            .ifBlank { "subtitles" }
        val file = File(dir, "$safe.srt")
        file.writeText(SubtitleFormatter.toSrt(segments), Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/x-subrip"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("subtitle", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
