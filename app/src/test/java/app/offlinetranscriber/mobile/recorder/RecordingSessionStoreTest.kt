package app.offlinetranscriber.mobile.recorder

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RecordingSessionStoreTest {

    @Before
    fun setUp() {
        RecordingSessionStore.reset()
    }

    @Test
    fun testInitialStateIsIdle() {
        val state = RecordingSessionStore.state.value
        assertEquals(RecordingStatus.IDLE, state.status)
        assertEquals(0L, state.durationMs)
        assertEquals(0f, state.amplitude, 0.001f)
    }

    @Test
    fun testStateTransitionsAndUpdates() {
        RecordingSessionStore.set(
            RecordingState(
                status = RecordingStatus.RECORDING,
                durationMs = 1200L,
                amplitude = 0.5f,
                filePath = "/tmp/test.m4a"
            )
        )

        val recording = RecordingSessionStore.state.value
        assertEquals(RecordingStatus.RECORDING, recording.status)
        assertEquals(1200L, recording.durationMs)
        assertEquals(0.5f, recording.amplitude, 0.001f)
        assertEquals("/tmp/test.m4a", recording.filePath)

        RecordingSessionStore.update {
            it.copy(status = RecordingStatus.PAUSED, amplitude = 0f)
        }

        val paused = RecordingSessionStore.state.value
        assertEquals(RecordingStatus.PAUSED, paused.status)
        assertEquals(0f, paused.amplitude, 0.001f)

        RecordingSessionStore.reset()
        val reset = RecordingSessionStore.state.value
        assertEquals(RecordingStatus.IDLE, reset.status)
    }
}
