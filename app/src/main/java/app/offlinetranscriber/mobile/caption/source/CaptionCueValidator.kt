package app.offlinetranscriber.mobile.caption.source

import app.offlinetranscriber.mobile.caption.model.CaptionCue

object CaptionCueValidator {

    fun validate(
        cues: List<CaptionCue>,
        durationMs: Long
    ): List<CaptionCue> {
        val maxUs = if (durationMs > 0L) durationMs * 1000L else Long.MAX_VALUE

        return cues
            .map { cue ->
                cue.copy(
                    startUs = cue.startUs.coerceIn(0L, maxUs),
                    endUs = cue.endUs.coerceIn(0L, maxUs),
                    text = cue.text
                        .replace(Regex("""\s+"""), " ")
                        .trim()
                )
            }
            .filter {
                it.text.isNotBlank() && it.endUs > it.startUs
            }
            .sortedWith(
                compareBy<CaptionCue> { it.startUs }.thenBy { it.endUs }
            )
    }
}
