package com.example.transcriber.backup.archive

import com.example.transcriber.backup.format.BackupManifestEntry
import com.example.transcriber.backup.restore.BackupZipSafety
import java.io.FilterOutputStream
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipEntryDigestWriter(
    private val zip: ZipOutputStream
) {

    suspend fun write(
        name: String,
        block: suspend (OutputStream) -> Unit
    ): BackupManifestEntry {
        BackupZipSafety.requireSafeName(name)

        zip.putNextEntry(ZipEntry(name))

        val digest = MessageDigest.getInstance("SHA-256")
        val counting = CountingOutputStream(NonClosingOutputStream(zip))
        val digesting = DigestOutputStream(counting, digest)

        try {
            block(digesting)
            digesting.flush()
        } finally {
            zip.closeEntry()
        }

        return BackupManifestEntry(
            name = name,
            uncompressedBytes = counting.count,
            sha256 = digest.digest().joinToString("") { "%02x".format(it) }
        )
    }
}

private class CountingOutputStream(
    output: OutputStream
) : FilterOutputStream(output) {

    var count: Long = 0L
        private set

    override fun write(b: Int) {
        out.write(b)
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        count += len.toLong()
    }

    override fun close() {
        flush()
    }
}
