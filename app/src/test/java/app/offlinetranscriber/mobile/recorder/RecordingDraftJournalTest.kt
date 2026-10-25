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

    private fun m4aBytes(payloadSize: Int): ByteArray {
        val header = byteArrayOf(
            0, 0, 0, 0x20,
            'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(),
            'M'.code.toByte(), '4'.code.toByte(), 'A'.code.toByte(), ' '.code.toByte()
        )
        return header + ByteArray(payloadSize)
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
        val audioFile = File(filesDir, "interrupted.m4a").apply {
            writeBytes(m4aBytes(4096))
        }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)
        RecordingDraftJournal.updateHeartbeat(filesDir, 12000L, isPaused = false)

        val recoverable = RecordingDraftJournal.getRecoverableDraft(filesDir)
        assertNotNull(recoverable)
        assertEquals(audioFile.absolutePath, recoverable?.filePath)
        assertEquals(12000L, recoverable?.durationMs)
    }

    @Test
    fun testGetRecoverableDraftRejectsM4aWithoutFtypBox() {
        val audioFile = File(filesDir, "invalid.m4a").apply { writeBytes(ByteArray(4096)) }
        RecordingDraftJournal.start(filesDir, audioFile.absolutePath, 1000L)

        assertNull(
            "A size-only M4A file must not be offered as recoverable",
            RecordingDraftJournal.getRecoverableDraft(filesDir)
        )
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

    @Test
    fun discardDoesNotDeleteOutsideAppRoot() {
        val externalFile = tempFolder.newFile("private.m4a").apply {
            writeBytes(ByteArray(2048))
        }
        RecordingDraftJournal.start(filesDir, externalFile.absolutePath, 1000L)
        RecordingDraftJournal.discard(filesDir)
        assertTrue("Journal must not delete files outside its allowed root", externalFile.exists())
        assertNull(RecordingDraftJournal.read(filesDir))
    }

    @Test
    fun recoveryDoesNotRepairUntrustedWavPath() {
        val outsideWav = tempFolder.newFile("outside.wav").apply {
            writeBytes(ByteArray(100))
        }
        RecordingDraftJournal.start(filesDir, outsideWav.absolutePath, 1000L)
        assertNull(RecordingDraftJournal.getRecoverableDraft(filesDir))
    }
}
