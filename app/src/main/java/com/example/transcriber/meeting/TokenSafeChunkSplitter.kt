package com.example.transcriber.meeting

object TokenSafeChunkSplitter {

    fun split(
        chunk: MeetingChunk
    ): Pair<MeetingChunk, MeetingChunk>? {
        if (chunk.segments.size < 2) return null

        val midpoint = chunk.segments.size / 2

        return MeetingChunk(
            index = chunk.index * 2,
            segments = chunk.segments.subList(0, midpoint)
        ) to MeetingChunk(
            index = chunk.index * 2 + 1,
            segments = chunk.segments.subList(midpoint, chunk.segments.size)
        )
    }
}
