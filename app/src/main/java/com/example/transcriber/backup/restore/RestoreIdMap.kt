package com.example.transcriber.backup.restore

class RestoreIdMap {

    val transcripts = mutableMapOf<Long, Long>()
    val segments = mutableMapOf<Long, Long>()
    val collections = mutableMapOf<Long, Long>()
    val studyPacks = mutableMapOf<Long, Long>()
    val askConversations = mutableMapOf<Long, Long>()
    val askMessages = mutableMapOf<Long, Long>()
    val meetingPacks = mutableMapOf<Long, Long>()
    val speakerClusters = mutableMapOf<Long, Long>()

    fun requireTranscript(oldId: Long): Long =
        checkNotNull(transcripts[oldId]) { "Missing restored transcript mapping for oldId=$oldId" }

    fun requireSegment(oldId: Long): Long =
        checkNotNull(segments[oldId]) { "Missing restored segment mapping for oldId=$oldId" }
}
