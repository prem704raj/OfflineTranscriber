package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment
import app.offlinetranscriber.mobile.meeting.nano.MeetingNanoValidator
import app.offlinetranscriber.mobile.meeting.nano.ParsedProtocolAction
import app.offlinetranscriber.mobile.meeting.nano.ParsedProtocolDecision
import app.offlinetranscriber.mobile.meeting.nano.ParsedProtocolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingNanoValidatorTest {

    @Test
    fun validatesSourceLabelsAndAssigneeGrounding() {
        val segments = listOf(
            MeetingSourceSegment(
                segmentId = 101L,
                transcriptId = 1L,
                startMs = 0L,
                endMs = 5000L,
                text = "Rahul will review the pull request by Friday."
            ),
            MeetingSourceSegment(
                segmentId = 102L,
                transcriptId = 1L,
                startMs = 5000L,
                endMs = 10000L,
                text = "We agreed to ship v1.1 next week."
            )
        )

        val chunk = MeetingChunk(index = 0, segments = segments)
        val labeled = MeetingSourceFormatter.format(chunk)

        val parsed = ParsedProtocolResult(
            summary = "Meeting summary.",
            summarySources = listOf("S1"),
            actions = listOf(
                // Valid assignee and due date matching segment text
                ParsedProtocolAction(
                    text = "Review PR",
                    assignee = "Rahul",
                    dueText = "Friday",
                    sourceLabel = "S1"
                ),
                // Hallucinated assignee "Alice" and hallucinated due date "Sunday" on segment S1
                ParsedProtocolAction(
                    text = "Another task",
                    assignee = "Alice",
                    dueText = "Sunday",
                    sourceLabel = "S1"
                ),
                // Invented non-existent label S99
                ParsedProtocolAction(
                    text = "Phantom action",
                    assignee = "",
                    dueText = "",
                    sourceLabel = "S99"
                )
            ),
            decisions = listOf(
                ParsedProtocolDecision(
                    text = "Ship v1.1 next week",
                    sourceLabel = "S2"
                )
            ),
            questions = emptyList(),
            topics = emptyList()
        )

        val pack = MeetingNanoValidator.validateProtocol(parsed, chunk, labeled)

        // Only S1 and S2 actions accepted (S99 dropped)
        assertEquals(2, pack.actions.size)

        // First action has verified Rahul and Friday
        assertEquals("Rahul", pack.actions[0].assignee)
        assertEquals("Friday", pack.actions[0].dueText)

        // Second action had Alice and Sunday stripped because they don't appear in source
        assertEquals("", pack.actions[1].assignee)
        assertEquals("", pack.actions[1].dueText)

        // Decision verified
        assertEquals(1, pack.decisions.size)
        assertEquals(102L, pack.decisions[0].sourceSegmentId)
    }
}
