package com.example.transcriber.queue

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionQueueTest {

    @Test
    fun transcriptionJobStatusValues() {
        val statuses = TranscriptionJobStatus.entries.map { it.name }
        assertEquals(
            listOf("QUEUED", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"),
            statuses
        )
    }

    @Test
    fun transcriptionSourceTypeValues() {
        val types = TranscriptionSourceType.entries.map { it.name }
        assertEquals(
            listOf("AUDIO", "VIDEO", "RECORDING"),
            types
        )
    }
}
