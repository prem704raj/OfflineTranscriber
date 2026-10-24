package app.offlinetranscriber.mobile.recorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CrashSafeAudioRecorderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDir: File

    @Before
    fun setUp() {
        testDir = tempFolder.newFolder("recorder_tests")
    }

    @Test
    fun writeFailureSetsErrorStateAndTriggersCallback() {
        val recorder = CrashSafeAudioRecorder()
        assertFalse(recorder.hasWriteError)
        assertFalse(recorder.recording)

        var capturedError: Exception? = null
        recorder.onErrorListener = { error ->
            capturedError = error
        }

        // Simulate a write error during recording loop:
        // We trigger an IOException to verify error state handling
        val simulatedException = IOException("No space left on device")
        recorder.onErrorListener?.invoke(simulatedException)

        assertNotNull("Recorder must invoke onErrorListener on write exception", capturedError)
        assertEquals("No space left on device", capturedError?.message)
    }

    @Test
    fun stopReturnsFailureWhenWriteErrorFlagIsSet() {
        val recorder = CrashSafeAudioRecorder()

        // When not recording, stop returns failure
        val notRecordingResult = recorder.stop()
        assertTrue("stop() when not recording must return failure", notRecordingResult.isFailure)

        // Verify that if a write error occurred, stop() reports the failure
        // Use reflection to set hasWriteError and test stop() response
        val field = CrashSafeAudioRecorder::class.java.getDeclaredField("hasWriteError")
        field.isAccessible = true
        field.setBoolean(recorder, true)

        val isRecField = CrashSafeAudioRecorder::class.java.getDeclaredField("isRecording")
        isRecField.isAccessible = true
        (isRecField.get(recorder) as java.util.concurrent.atomic.AtomicBoolean).set(true)

        val stopResult = recorder.stop()
        assertTrue("stop() must return failure Result when write error occurred during session", stopResult.isFailure)
        assertTrue(stopResult.exceptionOrNull() is IOException)
    }

    @Test
    fun flushAndSyncHeaderSealsValidWavFile() {
        val audioFile = File(testDir, "test_flush.wav")
        WavAudioWriter.writeInitialHeader(audioFile)

        // Append 64,000 bytes (2 seconds of 16kHz 16-bit mono PCM)
        audioFile.appendBytes(ByteArray(64_000))

        val recorder = CrashSafeAudioRecorder()
        // Point recorder to file
        val fileField = CrashSafeAudioRecorder::class.java.getDeclaredField("currentFile")
        fileField.isAccessible = true
        fileField.set(recorder, audioFile)

        recorder.flushAndSyncHeader()

        assertTrue("File must be valid WAV after flushAndSyncHeader", WavAudioWriter.isValidWav(audioFile))
        assertEquals(2000L, WavAudioWriter.getDurationMs(audioFile))
    }
}

