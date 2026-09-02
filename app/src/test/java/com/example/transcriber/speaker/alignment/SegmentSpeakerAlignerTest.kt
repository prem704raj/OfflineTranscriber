package com.example.transcriber.speaker.alignment

import com.example.transcriber.data.model.TranscriptSegmentEntity
import com.example.transcriber.speaker.model.GlobalSpeakerTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentSpeakerAlignerTest {

    @Test
    fun testDominantSingleSpeakerAssigned() {
        val segment = TranscriptSegmentEntity(
            id = 101L,
            transcriptId = 1L,
            startMs = 1000L,
            endMs = 5000L, // 4000ms duration
            text = "Hello world"
        )
        val turns = listOf(
            GlobalSpeakerTurn(globalSpeakerIndex = 0, startMs = 1000L, endMs = 4500L) // 3500ms overlap -> 87.5%
        )
        val clusterMap = mapOf(0 to 555L)

        val matches = SegmentSpeakerAligner.alignSegmentsToSpeakers(
            segments = listOf(segment),
            turns = turns,
            speakerClusterMap = clusterMap
        )

        assertEquals(1, matches.size)
        assertEquals(555L, matches[0].speakerClusterId)
        assertEquals(0.875f, matches[0].overlapRatio, 0.01f)
    }

    @Test
    fun testAmbiguousSpeakersYieldNullAssignment() {
        val segment = TranscriptSegmentEntity(
            id = 102L,
            transcriptId = 1L,
            startMs = 0L,
            endMs = 4000L,
            text = "Overlapping conversation"
        )
        // Speaker 0 speaks for 2000ms (50%), Speaker 1 speaks for 1800ms (45%) -> margin is 5% < 10%
        val turns = listOf(
            GlobalSpeakerTurn(globalSpeakerIndex = 0, startMs = 0L, endMs = 2000L),
            GlobalSpeakerTurn(globalSpeakerIndex = 1, startMs = 2000L, endMs = 3800L)
        )
        val clusterMap = mapOf(0 to 10L, 1 to 20L)

        val matches = SegmentSpeakerAligner.alignSegmentsToSpeakers(
            segments = listOf(segment),
            turns = turns,
            speakerClusterMap = clusterMap
        )

        assertEquals(1, matches.size)
        assertNull(matches[0].speakerClusterId) // Ambiguous, left unassigned
    }

    @Test
    fun testNoOverlappingTurnsYieldsNull() {
        val segment = TranscriptSegmentEntity(
            id = 103L,
            transcriptId = 1L,
            startMs = 10000L,
            endMs = 12000L,
            text = "Silence or music"
        )
        val turns = listOf(
            GlobalSpeakerTurn(globalSpeakerIndex = 0, startMs = 0L, endMs = 5000L)
        )
        val clusterMap = mapOf(0 to 10L)

        val matches = SegmentSpeakerAligner.alignSegmentsToSpeakers(
            segments = listOf(segment),
            turns = turns,
            speakerClusterMap = clusterMap
        )

        assertEquals(1, matches.size)
        assertNull(matches[0].speakerClusterId)
        assertEquals(0.0f, matches[0].overlapRatio, 0.01f)
    }
}
