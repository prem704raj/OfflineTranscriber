package com.example.transcriber.backup.crypto

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCrypto(
    private val secureRandom: SecureRandom = SecureRandom()
) {

    fun isEncrypted(input: BufferedInputStream): Boolean {
        input.mark(BackupCryptoConstants.MAGIC.size + 1)
        val probe = ByteArray(BackupCryptoConstants.MAGIC.size)
        val count = input.read(probe)
        input.reset()
        return count == probe.size && probe.contentEquals(BackupCryptoConstants.MAGIC)
    }

    fun createEncryptingStream(
        output: OutputStream,
        password: CharArray,
        iterations: Int = BackupCryptoConstants.PBKDF2_ITERATIONS
    ): CipherOutputStream {
        require(
            iterations in BackupCryptoConstants.MIN_ITERATIONS..BackupCryptoConstants.MAX_ITERATIONS
        ) { "Invalid PBKDF2 iterations: $iterations" }

        val salt = ByteArray(BackupCryptoConstants.SALT_BYTES).also {
            secureRandom.nextBytes(it)
        }

        val nonce = ByteArray(BackupCryptoConstants.NONCE_BYTES).also {
            secureRandom.nextBytes(it)
        }

        val header = encodeHeader(
            iterations = iterations,
            salt = salt,
            nonce = nonce
        )

        output.write(header)

        val keyBytes = deriveKey(
            password = password,
            salt = salt,
            iterations = iterations
        )

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(BackupCryptoConstants.GCM_TAG_BITS, nonce)
            )
            cipher.updateAAD(header)

            return CipherOutputStream(output, cipher)
        } finally {
            keyBytes.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    fun decryptToFile(
        input: InputStream,
        password: CharArray,
        outputZip: File,
        onBytes: (Long) -> Unit = {}
    ) {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input, 64 * 1024)
        val header = readHeader(buffered)

        val keyBytes = deriveKey(
            password = password,
            salt = header.salt,
            iterations = header.iterations
        )

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(BackupCryptoConstants.GCM_TAG_BITS, header.nonce)
            )
            cipher.updateAAD(header.encodedHeader)

            outputZip.parentFile?.mkdirs()

            FileOutputStream(outputZip).buffered(64 * 1024).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L

                while (true) {
                    val read = buffered.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue

                    val plain = cipher.update(buffer, 0, read)
                    if (plain != null && plain.isNotEmpty()) {
                        output.write(plain)
                    }
                    total += read
                    onBytes(total)
                }

                val finalBytes = cipher.doFinal()
                if (finalBytes != null && finalBytes.isNotEmpty()) {
                    output.write(finalBytes)
                }
                output.flush()
            }
        } catch (error: AEADBadTagException) {
            outputZip.delete()
            throw InvalidBackupPasswordException()
        } catch (error: BadPaddingException) {
            outputZip.delete()
            throw InvalidBackupPasswordException()
        } catch (error: Throwable) {
            outputZip.delete()
            throw error
        } finally {
            keyBytes.fill(0)
            header.salt.fill(0)
            header.nonce.fill(0)
        }
    }

    fun readHeader(input: InputStream): BackupEncryptedHeader {
        val data = DataInputStream(input)
        val magic = ByteArray(BackupCryptoConstants.MAGIC.size)
        data.readFully(magic)

        require(magic.contentEquals(BackupCryptoConstants.MAGIC)) {
            "Not an encrypted Offline Transcriber backup."
        }

        val version = data.readInt()
        val kdf = data.readInt()
        val iterations = data.readInt()
        val saltLength = data.readInt()
        val nonceLength = data.readInt()

        require(version == BackupCryptoConstants.ENVELOPE_VERSION) {
            "Unsupported encrypted backup version: $version"
        }

        require(kdf == BackupCryptoConstants.KDF_PBKDF2_SHA256) {
            "Unsupported backup key derivation: $kdf"
        }

        require(
            iterations in BackupCryptoConstants.MIN_ITERATIONS..BackupCryptoConstants.MAX_ITERATIONS
        ) { "PBKDF2 iterations out of safe bounds: $iterations" }

        require(saltLength in 16..64) {
            "Invalid salt length: $saltLength"
        }

        require(nonceLength in 12..32) {
            "Invalid nonce length: $nonceLength"
        }

        val salt = ByteArray(saltLength)
        val nonce = ByteArray(nonceLength)
        data.readFully(salt)
        data.readFully(nonce)

        val encoded = encodeHeader(
            envelopeVersion = version,
            kdfId = kdf,
            iterations = iterations,
            salt = salt,
            nonce = nonce
        )

        return BackupEncryptedHeader(
            envelopeVersion = version,
            kdfId = kdf,
            iterations = iterations,
            salt = salt,
            nonce = nonce,
            encodedHeader = encoded
        )
    }

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        require(password.size >= BackupPasswordPolicy.MIN_LENGTH) {
            "Backup password is too short."
        }

        val spec = PBEKeySpec(
            password,
            salt,
            iterations,
            BackupCryptoConstants.KEY_BITS
        )

        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeHeader(
        iterations: Int,
        salt: ByteArray,
        nonce: ByteArray
    ): ByteArray = encodeHeader(
        envelopeVersion = BackupCryptoConstants.ENVELOPE_VERSION,
        kdfId = BackupCryptoConstants.KDF_PBKDF2_SHA256,
        iterations = iterations,
        salt = salt,
        nonce = nonce
    )

    private fun encodeHeader(
        envelopeVersion: Int,
        kdfId: Int,
        iterations: Int,
        salt: ByteArray,
        nonce: ByteArray
    ): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { data ->
            data.write(BackupCryptoConstants.MAGIC)
            data.writeInt(envelopeVersion)
            data.writeInt(kdfId)
            data.writeInt(iterations)
            data.writeInt(salt.size)
            data.writeInt(nonce.size)
            data.write(salt)
            data.write(nonce)
            data.flush()
        }
        return bytes.toByteArray()
    }
}

class InvalidBackupPasswordException : Exception(
    "Incorrect password or damaged encrypted backup."
)
