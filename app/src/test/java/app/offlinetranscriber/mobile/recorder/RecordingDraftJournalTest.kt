package app.offlinetranscriber.mobile.recorder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecordingDraftJournalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
    }

    @Test
    fun testStartAndReadDraft() {
        val audioFile = File(filesDir, "test.m4a").apply { writeBytes(ByteArray(2048)) }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)

        val draft = RecordingDraftJournal.read(filesDir)
        assertNotNull(draft)
        assertEquals(audioFile.absolutePath, draft?.filePath)
        assertEquals("RECORDING", draft?.status)
        assertEquals(1000L, draft?.startedAtMs)
    }

    @Test
    fun testHeartbeatUpdate() {
        val audioFile = File(filesDir, "test.m4a").apply { writeBytes(ByteArray(2048)) }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)
        RecordingDraftJournal.updateHeartbeat(filesDir, 5000L, isPaused = true)

        val draft = RecordingDraftJournal.read(filesDir)
        assertNotNull(draft)
        assertEquals(5000L, draft?.durationMs)
        assertEquals("PAUSED", draft?.status)
    }

    @Test
    fun testGetRecoverableDraftReturnsValidDraftWhenAudioExists() {
        val audioFile = File(filesDir, "interrupted.m4a").apply { writeBytes(ByteArray(4096)) }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)
        RecordingDraftJournal.updateHeartbeat(filesDir, 12000L, isPaused = false)

        val recoverable = RecordingDraftJournal.getRecoverableDraft(filesDir)
        assertNotNull(recoverable)
        assertEquals(audioFile.absolutePath, recoverable?.filePath)
        assertEquals(12000L, recoverable?.durationMs)
    }

    @Test
    fun testGetRecoverableDraftReturnsNullWhenAudioMissing() {
        val missingAudio = File(filesDir, "non_existent.m4a")
        RecordingDraftJournal.start(filesDir, missingAudio.absolutePath, 1000L)

        val recoverable = RecordingDraftJournal.getRecoverableDraft(filesDir)
        assertNull("Missing audio file must not be offered as recoverable", recoverable)
    }

    @Test
    fun testCompleteClearsJournal() {
        val audioFile = File(filesDir, "test.m4a").apply { writeBytes(ByteArray(2048)) }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)
        RecordingDraftJournal.complete(filesDir, 5000L)

        val draft = RecordingDraftJournal.read(filesDir)
        assertNull(draft)
    }

    @Test
    fun testDiscardDeletesAudioAndClearsJournal() {
        val audioFile = File(filesDir, "test.m4a").apply { writeBytes(ByteArray(2048)) }
        assertTrue(audioFile.exists())

        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)
        RecordingDraftJournal.discard(filesDir)

        val draft = RecordingDraftJournal.read(filesDir)
        assertNull(draft)
        assertFalse("Audio file must be deleted on discard", audioFile.exists())
    }
}
