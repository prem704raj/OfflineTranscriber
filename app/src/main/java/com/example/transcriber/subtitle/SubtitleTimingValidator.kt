package com.example.transcriber.subtitle

object SubtitleTimingValidator {
    data class Result(val valid: Boolean, val message: String? = null)

    fun validate(
        startMs: Long,
        endMs: Long,
        previousEndMs: Long?,
        nextStartMs: Long?
    ): Result {
        if (startMs < 0L) return Result(false, "Start time can't be negative.")
        if (endMs <= startMs) return Result(false, "End time must be after start time.")
        if (endMs - startMs < 120L) return Result(false, "Subtitle is too short.")
        if (previousEndMs != null && startMs < previousEndMs - 1_500L) {
            return Result(false, "Start overlaps the previous subtitle too much.")
        }
        if (nextStartMs != null && endMs > nextStartMs + 1_500L) {
            return Result(false, "End overlaps the next subtitle too much.")
        }
        return Result(true)
    }
}
