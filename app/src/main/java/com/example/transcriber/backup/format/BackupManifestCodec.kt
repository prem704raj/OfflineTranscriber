package com.example.transcriber.backup.format

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class BackupManifestCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(manifest: BackupManifest): String =
        json.encodeToString(manifest)

    fun decode(text: String): BackupManifest =
        json.decodeFromString(text)

    fun write(manifest: BackupManifest, output: OutputStream) {
        val text = encode(manifest)
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    fun read(input: InputStream): BackupManifest {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return decode(text)
    }

    fun validateCompatibility(manifest: BackupManifest) {
        require(manifest.product == BackupFormat.PRODUCT) {
            "This is not an Offline Transcriber backup."
        }

        require(manifest.formatVersion in 1..BackupFormat.FORMAT_VERSION) {
            if (manifest.formatVersion > BackupFormat.FORMAT_VERSION) {
                "This backup was created by a newer version of Offline Transcriber."
            } else {
                "Unsupported backup format."
            }
        }

        require(manifest.backupId.isNotBlank()) {
            "Backup ID is missing from manifest."
        }
    }
}
