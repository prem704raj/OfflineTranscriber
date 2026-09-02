package com.example.transcriber.speaker.engine

import com.example.transcriber.speaker.model.GlobalSpeakerTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerTurnNormalizerTest {

    @Test
    fun testDropsTooShortTurns() {
        val input = listOf(
            GlobalSpeakerTurn(0, 0L, 100L), // 100ms < 250ms -> dropped
            GlobalSpeakerTurn(0, 200L, 1000L) // 800ms -> kept
        )
        val result = SpeakerTurnNormalizer.normalize(input, minTurnDurationMs = 250L)
        assertEquals(1, result.size)
        assertEquals(200L, result[0].startMs)
        assertEquals(1000L, result[0].endMs)
    }

    @Test
    fun testMergesSameSpeakerWithSmallGap() {
        val input = listOf(
            GlobalSpeakerTurn(0, 0L, 1000L),
            GlobalSpeakerTurn(0, 1200L, 2000L) // Gap is 200ms <= 400ms -> merged
        )
        val result = SpeakerTurnNormalizer.normalize(input, maxMergeGapMs = 400L)
        assertEquals(1, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals(2000L, result[0].endMs)
        assertEquals(0, result[0].globalSpeakerIndex)
    }

    @Test
    fun testDoesNotMergeDifferentSpeakers() {
        val input = listOf(
            GlobalSpeakerTurn(0, 0L, 1000L),
            GlobalSpeakerTurn(1, 1100L, 2000L)
        )
        val result = SpeakerTurnNormalizer.normalize(input)
        assertEquals(2, result.size)
        assertEquals(0, result[0].globalSpeakerIndex)
        assertEquals(1, result[1].globalSpeakerIndex)
    }

    @Test
    fun testClipsInterSpeakerOverlap() {
        val input = listOf(
            GlobalSpeakerTurn(0, 0L, 2000L),
            GlobalSpeakerTurn(1, 1500L, 3000L) // Overlaps from 1500 to 2000
        )
        val result = SpeakerTurnNormalizer.normalize(input)
        assertEquals(2, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals(1500L, result[0].endMs)
        assertEquals(1500L, result[1].startMs)
        assertEquals(3000L, result[1].endMs)
    }

    @Test
    fun testRenumberIndicesSequentially() {
        val input = listOf(
            GlobalSpeakerTurn(5, 0L, 1000L),
            GlobalSpeakerTurn(9, 1500L, 2500L),
            GlobalSpeakerTurn(5, 3000L, 4000L)
        )
        val result = SpeakerTurnNormalizer.normalize(input)
        assertEquals(3, result.size)
        assertEquals(0, result[0].globalSpeakerIndex)
        assertEquals(1, result[1].globalSpeakerIndex)
        assertEquals(0, result[2].globalSpeakerIndex)
    }
}
