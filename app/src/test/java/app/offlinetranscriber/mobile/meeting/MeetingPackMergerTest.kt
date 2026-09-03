package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingAction
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingDecision
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingPack
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingQuestion
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingSummary
import app.offlinetranscriber.mobile.meeting.model.GeneratedMeetingTopic
import app.offlinetranscriber.mobile.meeting.model.MeetingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingPackMergerTest {

    @Test
    fun deduplicatesActionsAndPreservesTimelineOrder() {
        val enhanced1 = GeneratedMeetingPack(
            summary = GeneratedMeetingSummary("Chunk 1 summary", listOf(1L)),
            actions = listOf(
                GeneratedMeetingAction("Send launch report to the team", "Rahul", "Friday", 1L, 1000L)
            ),
            decisions = listOf(
                GeneratedMeetingDecision("Use SQLite Room", 1L, 1000L)
            ),
            questions = emptyList(),
            topics = listOf(
                GeneratedMeetingTopic("Launch Plan", 1L, 1000L)
            ),
            engine = MeetingEngine.GEMINI_NANO_PROTOCOL
        )

        val enhanced2 = GeneratedMeetingPack(
            summary = GeneratedMeetingSummary("Chunk 2 summary", listOf(2L)),
            actions = listOf(
                // Similar to enhanced1 action -> will be deduplicated
                GeneratedMeetingAction("Send launch report to team", "Rahul", "Friday", 1L, 1000L),
                GeneratedMeetingAction("Verify migration test", "", "", 2L, 5000L)
            ),
            decisions = emptyList(),
            questions = listOf(
                GeneratedMeetingQuestion("Who owns testing?", 2L, 5000L)
            ),
            topics = listOf(
                GeneratedMeetingTopic("Migration Strategy", 2L, 5000L)
            ),
            engine = MeetingEngine.GEMINI_NANO_PROTOCOL
        )

        val classic = GeneratedMeetingPack(
            summary = GeneratedMeetingSummary("Classic baseline summary", listOf(1L, 2L)),
            actions = emptyList(),
            decisions = emptyList(),
            questions = emptyList(),
            topics = emptyList(),
            engine = MeetingEngine.CLASSIC
        )

        val merged = MeetingPackMerger.merge(listOf(enhanced1, enhanced2), classic)

        assertEquals(MeetingEngine.GEMINI_NANO_PROTOCOL, merged.engine)
        // Deduplicated from 3 to 2
        assertEquals(2, merged.actions.size)
        assertEquals(1, merged.decisions.size)
        assertEquals(1, merged.questions.size)
        assertEquals(2, merged.topics.size)
        assertTrue(merged.summary.text.contains("Chunk 1 summary"))
    }
}
