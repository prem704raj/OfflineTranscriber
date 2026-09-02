package com.example.transcriber.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareImportResultTest {

    @Test
    fun totalRejectedSumsAllRejectionCategories() {
        val result = ShareImportResult(
            enqueuedCount = 2,
            videoProRejected = 1,
            queueLimitRejected = 2,
            unsupportedOrFailed = 1,
            needsModel = false
        )

        assertEquals(4, result.totalRejected)
        assertEquals(2, result.enqueuedCount)
        assertFalse(result.needsModel)
    }

    @Test
    fun cleanImportHasZeroRejections() {
        val result = ShareImportResult(
            enqueuedCount = 3,
            videoProRejected = 0,
            queueLimitRejected = 0,
            unsupportedOrFailed = 0,
            needsModel = true
        )

        assertEquals(0, result.totalRejected)
        assertTrue(result.needsModel)
    }
}
