package com.example.transcriber.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageManagerTest {

    @Test
    fun storageUsageCalculatesTotalCorrectly() {
        val usage = StorageUsage(
            modelsBytes = 60_000_000L,
            recordingsBytes = 15_000_000L,
            extractedAudioBytes = 25_000_000L,
            temporaryBytes = 5_000_000L
        )

        assertEquals(105_000_000L, usage.totalKnownBytes)
    }
}
