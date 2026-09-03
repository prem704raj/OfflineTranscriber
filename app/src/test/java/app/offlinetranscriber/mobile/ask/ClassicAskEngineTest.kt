package app.offlinetranscriber.mobile.ask

import app.offlinetranscriber.mobile.ask.model.AskEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicAskEngineTest {

    @Test
    fun emptyEvidenceIsInsufficient() {
        val answer = ClassicAskEngine()
            .answer(
                "What is normalization?",
                emptyList()
            )

        assertTrue(answer.insufficientEvidence)
        assertTrue(answer.citations.isEmpty())
    }

    @Test
    fun groundedAnswerHasCitation() {
        val answer = ClassicAskEngine()
            .answer(
                "What is normalization?",
                listOf(
                    AskEvidence(
                        segmentId = 1,
                        transcriptId = 2,
                        transcriptTitle = "DBMS Lecture",
                        mediaType = "AUDIO",
                        startMs = 12_000L,
                        endMs = 20_000L,
                        text = "Normalization is a process used to reduce data redundancy in relational databases.",
                        rank = 0
                    )
                )
            )

        assertFalse(answer.insufficientEvidence)
        assertEquals(1, answer.citations.size)
        assertEquals("DBMS Lecture", answer.citations[0].transcriptTitle)
        assertEquals(12_000L, answer.citations[0].startMs)
    }

    @Test
    fun picksMostRelevantSegments() {
        val engine = ClassicAskEngine()
        val answer = engine.answer(
            "explain deadlock conditions",
            listOf(
                AskEvidence(
                    segmentId = 10,
                    transcriptId = 1,
                    transcriptTitle = "OS Lecture",
                    mediaType = "AUDIO",
                    startMs = 0L,
                    endMs = 5000L,
                    text = "Welcome to today's lecture on operating systems.",
                    rank = 1
                ),
                AskEvidence(
                    segmentId = 11,
                    transcriptId = 1,
                    transcriptTitle = "OS Lecture",
                    mediaType = "AUDIO",
                    startMs = 5000L,
                    endMs = 15000L,
                    text = "A deadlock occurs when processes hold resources and wait for each other in circular wait.",
                    rank = 0
                )
            )
        )

        assertFalse(answer.insufficientEvidence)
        assertTrue(answer.answer.contains("deadlock"))
    }
}
