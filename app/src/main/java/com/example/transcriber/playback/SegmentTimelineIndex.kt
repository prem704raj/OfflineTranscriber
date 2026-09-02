package com.example.transcriber.playback

import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.domain.model.TranscriptSegment

class SegmentTimelineIndex private constructor(
    private val entries: List<TimelineEntry>
) {
    data class TimelineEntry(
        val startMs: Long,
        val endMs: Long
    )

    companion object {
        fun from(
            input: List<TranscriptSegmentEntity>
        ) = SegmentTimelineIndex(
            input.map { TimelineEntry(it.startMs, it.endMs) }.sortedBy { it.startMs }
        )

        fun fromSegments(
            input: List<TranscriptSegment>
        ) = SegmentTimelineIndex(
            input.map { TimelineEntry(it.startMs, it.endMs) }.sortedBy { it.startMs }
        )
    }

    fun activeIndex(
        positionMs: Long,
        endGraceMs: Long = 250L
    ): Int {
        if (entries.isEmpty()) return -1

        var low = 0
        var high = entries.lastIndex
        var candidate = -1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val item = entries[mid]

            if (item.startMs <= positionMs) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        if (candidate < 0) return -1

        val item = entries[candidate]
        return if (positionMs <= item.endMs + endGraceMs) {
            candidate
        } else {
            -1
        }
    }
}
