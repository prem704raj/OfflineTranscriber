package com.example.transcriber.playback

import com.example.transcriber.domain.model.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentTimelineIndexTest {

    @Test
    fun emptyListReturnsMinusOne() {
        val index = SegmentTimelineIndex.fromSegments(emptyList())
        assertEquals(-1, index.activeIndex(0L))
        assertEquals(-1, index.activeIndex(5000L))
    }

    @Test
    fun exactHitFindsSegment() {
        val segments = listOf(
            TranscriptSegment(startMs = 0L, endMs = 2000L, text = "First"),
            TranscriptSegment(startMs = 2500L, endMs = 4500L, text = "Second"),
            TranscriptSegment(startMs = 5000L, endMs = 8000L, text = "Third")
        )
        val index = SegmentTimelineIndex.fromSegments(segments)

        assertEquals(0, index.activeIndex(0L))
        assertEquals(0, index.activeIndex(1000L))
        assertEquals(0, index.activeIndex(2000L))

        assertEquals(1, index.activeIndex(2500L))
        assertEquals(1, index.activeIndex(3500L))

        assertEquals(2, index.activeIndex(5000L))
        assertEquals(2, index.activeIndex(7999L))
    }

    @Test
    fun endGraceWindowKeepsPreviousSegmentActive() {
        val segments = listOf(
            TranscriptSegment(startMs = 0L, endMs = 2000L, text = "First"),
            TranscriptSegment(startMs = 3000L, endMs = 5000L, text = "Second")
        )
        val index = SegmentTimelineIndex.fromSegments(segments)

        // 2100ms is within 250ms grace of 2000ms
        assertEquals(0, index.activeIndex(2100L, endGraceMs = 250L))

        // 2300ms is past 250ms grace of 2000ms and before 3000ms
        assertEquals(-1, index.activeIndex(2300L, endGraceMs = 250L))
    }

    @Test
    fun positionBeforeFirstOrAfterLast() {
        val segments = listOf(
            TranscriptSegment(startMs = 1000L, endMs = 3000L, text = "Only")
        )
        val index = SegmentTimelineIndex.fromSegments(segments)

        assertEquals(-1, index.activeIndex(0L))
        assertEquals(-1, index.activeIndex(500L))
        assertEquals(0, index.activeIndex(1000L))
        assertEquals(0, index.activeIndex(3000L))
        assertEquals(0, index.activeIndex(3200L, endGraceMs = 250L))
        assertEquals(-1, index.activeIndex(4000L, endGraceMs = 250L))
    }
}
