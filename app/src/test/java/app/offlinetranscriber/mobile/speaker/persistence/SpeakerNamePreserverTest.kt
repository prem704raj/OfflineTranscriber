package app.offlinetranscriber.mobile.speaker.persistence

import app.offlinetranscriber.mobile.data.database.SpeakerTurnRow
import app.offlinetranscriber.mobile.speaker.model.GlobalSpeakerTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerNamePreserverTest {

    @Test
    fun testPreservesUserNamedSpeakersAcrossRerun() {
        val oldTurns = listOf(
            SpeakerTurnRow(
                id = 1L,
                speakerClusterId = 100L,
                speakerIndex = 0,
                customName = "Dr. Alice",
                userNamed = true,
                startMs = 0L,
                endMs = 10000L
            ),
            SpeakerTurnRow(
                id = 2L,
                speakerClusterId = 101L,
                speakerIndex = 1,
                customName = "Bob Smith",
                userNamed = true,
                startMs = 10000L,
                endMs = 20000L
            )
        )

        val newTurns = listOf(
            // In new run, speaker index 0 still speaks from 0 to 9500
            GlobalSpeakerTurn(globalSpeakerIndex = 0, startMs = 0L, endMs = 9500L),
            // Speaker index 1 speaks from 9800 to 20000
            GlobalSpeakerTurn(globalSpeakerIndex = 1, startMs = 9800L, endMs = 20000L)
        )

        val preserved = SpeakerNamePreserver.matchPreviousNames(oldTurns, newTurns)

        assertEquals(2, preserved.size)
        assertEquals("Dr. Alice", preserved[0]?.customName)
        assertTrue(preserved[0]?.userNamed == true)
        assertEquals("Bob Smith", preserved[1]?.customName)
        assertTrue(preserved[1]?.userNamed == true)
    }

    @Test
    fun testIgnoresNonNamedDefaultSpeakers() {
        val oldTurns = listOf(
            SpeakerTurnRow(
                id = 1L,
                speakerClusterId = 100L,
                speakerIndex = 0,
                customName = "Speaker 1",
                userNamed = false,
                startMs = 0L,
                endMs = 10000L
            )
        )

        val newTurns = listOf(
            GlobalSpeakerTurn(globalSpeakerIndex = 0, startMs = 0L, endMs = 10000L)
        )

        val preserved = SpeakerNamePreserver.matchPreviousNames(oldTurns, newTurns)
        assertTrue(preserved.isEmpty())
    }
}
