package app.offlinetranscriber.mobile.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoFollowControllerTest {

    @Test
    fun autoFollowSuppressesThenResumes() {
        var currentTime = 1000L
        val controller = AutoFollowController(
            suppressionMs = 4000L,
            timeProvider = { currentTime }
        )

        // Initially active
        assertTrue(controller.shouldAutoFollow())

        // User scrolls at t=1000
        controller.onUserScroll()
        assertFalse(controller.shouldAutoFollow())

        // At t=3000 (2s later) -> still suppressed
        currentTime = 3000L
        assertFalse(controller.shouldAutoFollow())

        // At t=5000 (4s later) -> suppression expired
        currentTime = 5000L
        assertTrue(controller.shouldAutoFollow())
    }

    @Test
    fun forceResumeImmediatelyRestoresAutoFollow() {
        var currentTime = 1000L
        val controller = AutoFollowController(
            suppressionMs = 4000L,
            timeProvider = { currentTime }
        )

        controller.onUserScroll()
        assertFalse(controller.shouldAutoFollow())

        controller.forceResume()
        assertTrue(controller.shouldAutoFollow())
    }
}
