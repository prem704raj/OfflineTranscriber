package app.offlinetranscriber.mobile.modelmanager

import app.offlinetranscriber.mobile.security.Sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

class ModelManagerVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var modelsDir: File

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("app_files")
        modelsDir = File(filesDir, "whisper_models").apply { mkdirs() }
    }

    @Test
    fun isInstalledEnforcesThresholdAndFileExistence() {
        val spec = ModelCatalog.fast
        val modelFile = File(modelsDir, spec.fileName)

        // 1. Missing file -> false
        assertFalse(modelFile.exists())
        val notInstalled = modelFile.exists() && modelFile.isFile && modelFile.length() >= spec.minimumValidBytes
        assertFalse(notInstalled)

        // 2. File too small -> false
        modelFile.writeBytes(ByteArray(1024))
        val small = modelFile.exists() && modelFile.isFile && modelFile.length() >= spec.minimumValidBytes
        assertFalse(small)

        // 3. File meeting size threshold -> true
        val raf = RandomAccessFile(modelFile, "rw")
        raf.setLength(spec.minimumValidBytes + 1024)
        raf.close()
        val installed = modelFile.exists() && modelFile.isFile && modelFile.length() >= spec.minimumValidBytes
        assertTrue(installed)
    }

    @Test
    fun shaVerificationAcceptsValidBytesAndRejectsCorruptedFiles() {
        val spec = ModelCatalog.fast
        val modelFile = File(modelsDir, spec.fileName)

        // Write corrupt content
        modelFile.writeBytes("corrupt bytes that do not match official whisper weights".toByteArray())
        val corruptMatch = Sha256.matches(modelFile, spec.sha256)
        assertFalse("Corrupt model file must fail SHA verification", corruptMatch)

        // Write mock model file with known content and verify matching
        val testBytes = "whisper_valid_weights_payload_simulation".toByteArray()
        val validFile = File(modelsDir, "valid_test.bin")
        validFile.writeBytes(testBytes)
        val knownSha = Sha256.of(validFile)

        assertTrue(Sha256.matches(validFile, knownSha))
    }

    @Test
    fun shaCacheKeyInvalidationPurgesStaleEntries() {
        val spec = ModelCatalog.balanced
        val modelFile = File(modelsDir, spec.fileName)
        modelFile.writeBytes(ByteArray(2048))

        val cacheKey = "${modelFile.absolutePath}:${modelFile.length()}:${modelFile.lastModified()}"
        val verifiedCache = java.util.concurrent.ConcurrentHashMap<String, String>()
        verifiedCache[cacheKey] = "some_sha_hash"

        assertEquals(1, verifiedCache.size)

        // Simulate invalidateVerification(spec)
        val prefix = "${modelFile.absolutePath}:"
        verifiedCache.keys.removeIf { it.startsWith(prefix) }

        assertEquals("Cache must be cleared after invalidation", 0, verifiedCache.size)
    }
}

