package app.offlinetranscriber.mobile.caption.export

import app.offlinetranscriber.mobile.caption.model.CaptionExportRequest
import app.offlinetranscriber.mobile.caption.model.CaptionExportResolution
import app.offlinetranscriber.mobile.caption.model.CaptionProjectSnapshot
import app.offlinetranscriber.mobile.caption.model.CaptionStyle

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
