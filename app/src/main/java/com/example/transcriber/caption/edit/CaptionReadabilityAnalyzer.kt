package com.example.transcriber.caption.edit

import com.example.transcriber.caption.model.CaptionCue

enum class CaptionReadability {
    GOOD,
    LONG,
    VERY_LONG,
    FAST
}

object CaptionReadabilityAnalyzer {

    fun analyze(
        cue: CaptionCue,
        maxLines: Int
    ): Set<CaptionReadability> {
        val text = cue.text
            .replace(Regex("""\s+"""), " ")
            .trim()

        val capacity = 42 * maxLines.coerceIn(1, 4)
        val durationSec = (cue.endUs - cue.startUs).coerceAtLeast(1L).toDouble() / 1_000_000.0
        val cps = if (durationSec > 0.0) text.length / durationSec else 0.0

        val result = mutableSetOf<CaptionReadability>()

        when {
            text.length <= capacity ->
                result += CaptionReadability.GOOD

            text.length <= (capacity * 1.35f) ->
                result += CaptionReadability.LONG

            else ->
                result += CaptionReadability.VERY_LONG
        }

        if (cps > 24.0) {
            result += CaptionReadability.FAST
        }

        return result
    }
}
