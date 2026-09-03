package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.classic.ClassicMeetingEngine
import app.offlinetranscriber.mobile.meeting.model.MeetingEngine
import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicMeetingEngineTest {

    @Test
    fun extractsActionsDecisionsAndQuestionsFromCues() {
        val segments = listOf(
            MeetingSourceSegment(
                segmentId = 1L,
                transcriptId = 1L,
                startMs = 0L,
                endMs = 5000L,
                text = "We decided to launch the new feature on Monday."
            ),
            MeetingSourceSegment(
                segmentId = 2L,
                transcriptId = 1L,
                startMs = 5000L,
                endMs = 10000L,
                text = "We need to follow up with the design team before release."
            ),
            MeetingSourceSegment(
                segmentId = 3L,
                transcriptId = 1L,
                startMs = 10000L,
                endMs = 15000L,
                text = "There is an open question: whether we should support legacy export?"
            )
        )

        val engine = ClassicMeetingEngine()
        val pack = engine.generate(segments)

        assertEquals(MeetingEngine.CLASSIC, pack.engine)
        assertTrue(pack.decisions.isNotEmpty())
        assertTrue(pack.actions.isNotEmpty())
        assertTrue(pack.questions.isNotEmpty())
        assertTrue(pack.summary.text.isNotBlank())
    }

    @Test
    fun handlesEmptySegmentsGracefully() {
        val engine = ClassicMeetingEngine()
        val pack = engine.generate(emptyList())

        assertEquals(MeetingEngine.CLASSIC, pack.engine)
        assertTrue(pack.actions.isEmpty())
        assertTrue(pack.decisions.isEmpty())
        assertTrue(pack.questions.isEmpty())
        assertTrue(pack.topics.isEmpty())
    }
}
