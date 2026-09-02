package com.example.transcriber.meeting

import com.example.transcriber.meeting.model.MeetingSourceSegment

data class MeetingChunk(
    val index: Int,
    val segments: List<MeetingSourceSegment>
)
