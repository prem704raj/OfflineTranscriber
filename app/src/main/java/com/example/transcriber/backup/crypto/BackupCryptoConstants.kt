package com.example.transcriber.backup.crypto

object BackupCryptoConstants {

    val MAGIC = "OTBKENC1".toByteArray(Charsets.US_ASCII)

    const val ENVELOPE_VERSION = 1

    const val KDF_PBKDF2_SHA256 = 1

    const val PBKDF2_ITERATIONS = 210_000

    const val SALT_BYTES = 16

    const val NONCE_BYTES = 12

    const val KEY_BITS = 256

    const val GCM_TAG_BITS = 128

    const val MAX_ITERATIONS = 2_000_000

    const val MIN_ITERATIONS = 100_000
}
