package app.offlinetranscriber.mobile.backup

import app.offlinetranscriber.mobile.backup.crypto.BackupCrypto
import app.offlinetranscriber.mobile.backup.crypto.BackupCryptoConstants
import app.offlinetranscriber.mobile.backup.crypto.BackupPasswordPolicy
import app.offlinetranscriber.mobile.backup.crypto.BackupSecretVault
import app.offlinetranscriber.mobile.backup.crypto.InvalidBackupPasswordException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupCryptoTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val crypto = BackupCrypto()

    @Test
    fun testPasswordPolicy() {
        assertNotNull(BackupPasswordPolicy.validate("short", "short"))
        assertNotNull(BackupPasswordPolicy.validate("password123", "different123"))
        assertNull(BackupPasswordPolicy.validate("correct_password", "correct_password"))
    }

    @Test
    fun testSecretVaultZeroing() {
        val vault = BackupSecretVault()
        val pw = "secretPassphrase".toCharArray()
        vault.put("op1", pw)

        val retrieved = vault.take("op1")
        assertNotNull(retrieved)
        assertEquals("secretPassphrase", String(retrieved!!))

        vault.remove("op1")
        assertNull(vault.take("op1"))
    }

    @Test
    fun testEncryptAndDecryptRoundTrip() {
        val password = "SuperSecretPassword123".toCharArray()
        val plainText = "Hello Offline Transcriber Backup and Recovery!".toByteArray(Charsets.UTF_8)

        val encryptedBytes = ByteArrayOutputStream().use { out ->
            crypto.createEncryptingStream(out, password, iterations = 100_000).use { cipherOut ->
                cipherOut.write(plainText)
                cipherOut.flush()
            }
            out.toByteArray()
        }

        // Verify magic detection
        assertTrue(crypto.isEncrypted(BufferedInputStream(ByteArrayInputStream(encryptedBytes))))

        // Decrypt
        val decryptedFile = tempFolder.newFile("decrypted.bin")
        crypto.decryptToFile(
            input = ByteArrayInputStream(encryptedBytes),
            password = password,
            outputZip = decryptedFile
        )

        val resultBytes = decryptedFile.readBytes()
        assertArrayEquals(plainText, resultBytes)
    }

    @Test
    fun testInvalidPasswordThrowsException() {
        val correctPassword = "CorrectPassword123".toCharArray()
        val wrongPassword = "WrongPassword999".toCharArray()
        val plainText = "Secret payload".toByteArray(Charsets.UTF_8)

        val encryptedBytes = ByteArrayOutputStream().use { out ->
            crypto.createEncryptingStream(out, correctPassword, iterations = 100_000).use { cipherOut ->
                cipherOut.write(plainText)
            }
            out.toByteArray()
        }

        val decryptedFile = tempFolder.newFile("decrypted_invalid.bin")
        try {
            crypto.decryptToFile(
                input = ByteArrayInputStream(encryptedBytes),
                password = wrongPassword,
                outputZip = decryptedFile
            )
            fail("Expected InvalidBackupPasswordException")
        } catch (e: InvalidBackupPasswordException) {
            // Expected
            assertFalse(decryptedFile.exists())
        }
    }

    @Test
    fun testTamperedPayloadThrowsException() {
        val password = "StrongPassword789".toCharArray()
        val plainText = "Authentic payload that will be tampered".toByteArray(Charsets.UTF_8)

        val encryptedBytes = ByteArrayOutputStream().use { out ->
            crypto.createEncryptingStream(out, password, iterations = 100_000).use { cipherOut ->
                cipherOut.write(plainText)
            }
            out.toByteArray()
        }

        // Tamper with ciphertext byte near the end
        encryptedBytes[encryptedBytes.size - 5] = (encryptedBytes[encryptedBytes.size - 5].toInt() xor 0xFF).toByte()

        val decryptedFile = tempFolder.newFile("decrypted_tampered.bin")
        try {
            crypto.decryptToFile(
                input = ByteArrayInputStream(encryptedBytes),
                password = password,
                outputZip = decryptedFile
            )
            fail("Expected AEAD authentication failure")
        } catch (e: InvalidBackupPasswordException) {
            // Expected
            assertFalse(decryptedFile.exists())
        }
    }
}
