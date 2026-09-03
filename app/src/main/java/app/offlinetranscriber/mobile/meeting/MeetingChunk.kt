package app.offlinetranscriber.mobile.meeting

import app.offlinetranscriber.mobile.meeting.model.MeetingSourceSegment

data class MeetingChunk(
    val index: Int,
    val segments: List<MeetingSourceSegment>
)
