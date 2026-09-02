package com.example.transcriber.caption.export

import com.example.transcriber.caption.model.CaptionExportRequest
import com.example.transcriber.caption.model.CaptionExportResolution
import com.example.transcriber.caption.model.CaptionProjectSnapshot
import com.example.transcriber.caption.model.CaptionStyle

object CaptionExportRequestFactory {

    fun create(
        project: CaptionProjectSnapshot,
        style: CaptionStyle,
        resolution: CaptionExportResolution,
        title: String
    ): CaptionExportRequest {
        require(project.cues.isNotEmpty()) {
            "Project has no cues to export."
        }

        val base = title
            .replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(80)
            .ifBlank { "Offline Transcriber" }

        return CaptionExportRequest(
            transcriptId = project.transcriptId,
            sourceVideoUri = project.sourceVideoUri,
            style = style,
            resolution = resolution,
            outputFileName = "$base - Captioned.mp4",
            cues = project.cues
        )
    }
}
