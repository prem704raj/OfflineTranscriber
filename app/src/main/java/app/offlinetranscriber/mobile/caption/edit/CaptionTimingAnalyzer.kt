package app.offlinetranscriber.mobile.caption.edit

import app.offlinetranscriber.mobile.caption.model.CaptionCue

data class CaptionTimingIssue(
    val segmentId: Long,
    val message: String
)

object CaptionTimingAnalyzer {

    fun analyze(
        cues: List<CaptionCue>
    ): List<CaptionTimingIssue> {
        val sorted = cues.sortedBy { it.startUs }
        val output = mutableListOf<CaptionTimingIssue>()

        sorted.forEachIndexed { index, cue ->
            val duration = cue.endUs - cue.startUs
            if (duration < 300_000L) {
                output += CaptionTimingIssue(
                    cue.segmentId,
                    "Caption is shorter than 0.3 seconds."
                )
            }

            val next = sorted.getOrNull(index + 1)
            if (next != null) {
                val overlap = cue.endUs - next.startUs
                if (overlap > 250_000L) {
                    output += CaptionTimingIssue(
                        cue.segmentId,
                        "Caption overlaps the next caption."
                    )
                }
            }
        }

        return output
    }
}
