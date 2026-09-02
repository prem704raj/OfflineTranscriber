package com.example.transcriber.performance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottlerTest {

    @Test
    fun zeroAndHundredAlwaysEmit() {
        val throttler = ProgressThrottler(minIntervalMs = 10_000L)

        // 0% emits
        assertTrue(throttler.shouldEmit(0))

        // Same 0% does not emit again immediately
        assertFalse(throttler.shouldEmit(0))

        // 100% emits even before interval expires
        assertTrue(throttler.shouldEmit(100))
    }

    @Test
    fun resetClearsState() {
        val throttler = ProgressThrottler(minIntervalMs = 10_000L)

        assertTrue(throttler.shouldEmit(50))
        assertFalse(throttler.shouldEmit(50))

        throttler.reset()
        assertTrue(throttler.shouldEmit(50))
    }
}
