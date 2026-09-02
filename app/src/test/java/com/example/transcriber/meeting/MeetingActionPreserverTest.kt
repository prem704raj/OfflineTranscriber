package com.example.transcriber.meeting

import com.example.transcriber.data.model.MeetingActionEntity
import com.example.transcriber.meeting.model.GeneratedMeetingAction
import com.example.transcriber.meeting.model.MeetingActionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingActionPreserverTest {

    @Test
    fun preservesDoneStatusForMatchingActions() {
        val oldActions = listOf(
            MeetingActionEntity(
                id = 1L,
                meetingPackId = 10L,
                text = "Send the weekly summary email to stakeholders",
                assignee = "Rahul",
                dueText = "Friday",
                sourceSegmentId = 1L,
                startMs = 0L,
                status = MeetingActionStatus.DONE.name,
                manuallyEdited = false
            )
        )

        val freshActions = listOf(
            GeneratedMeetingAction(
                text = "Send weekly summary email to stakeholders",
                assignee = "Rahul",
                dueText = "Friday",
                sourceSegmentId = 1L,
                startMs = 0L
            ),
            GeneratedMeetingAction(
                text = "Review security logs",
                assignee = "",
                dueText = "",
                sourceSegmentId = 2L,
                startMs = 1000L
            )
        )

        val merged = MeetingActionPreserver.merge(oldActions, freshActions)

        assertEquals(2, merged.size)
        assertEquals(MeetingActionStatus.DONE, merged[0].status)
        assertEquals(MeetingActionStatus.OPEN, merged[1].status)
    }

    @Test
    fun preservesUnmatchedManuallyEditedActions() {
        val oldActions = listOf(
            MeetingActionEntity(
                id = 2L,
                meetingPackId = 10L,
                text = "Custom user added manual task",
                assignee = "Me",
                dueText = "Today",
                sourceSegmentId = null,
                startMs = 0L,
                status = MeetingActionStatus.OPEN.name,
                manuallyEdited = true
            )
        )

        val freshActions = listOf(
            GeneratedMeetingAction(
                text = "Standard generated task",
                assignee = "",
                dueText = "",
                sourceSegmentId = 1L,
                startMs = 0L
            )
        )

        val merged = MeetingActionPreserver.merge(oldActions, freshActions)

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.generated.text == "Custom user added manual task" && it.manuallyEdited })
    }
}
