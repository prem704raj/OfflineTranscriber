package com.example.transcriber.backup.restore

import com.example.transcriber.backup.format.BackupFormat
import com.example.transcriber.backup.format.BackupManifest
import com.example.transcriber.backup.format.BackupManifestCodec
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class BackupArchiveValidator(
    private val manifestCodec: BackupManifestCodec,
    private val structureValidator: BackupStructureValidator
) {

    fun validate(
        sessionId: String,
        file: File,
        encrypted: Boolean,
        onProgress: (Int) -> Unit = {}
    ): BackupInspection {
        ZipFile(file).use { zip ->
            val entriesList = mutableListOf<ZipEntry>()
            val enumeration = zip.entries()
            while (enumeration.hasMoreElements()) {
                entriesList.add(enumeration.nextElement())
            }

            require(entriesList.size <= BackupSafetyLimits.MAX_ZIP_ENTRIES) {
                "Backup contains too many files: ${entriesList.size}"
            }

            val names = HashSet<String>()
            entriesList.forEach { entry ->
                BackupZipSafety.requireSafeName(entry.name)
                require(names.add(entry.name)) {
                    "Backup contains duplicate file entries: ${entry.name}"
                }
                require(!entry.isDirectory) {
                    "Unexpected directory entry in backup: ${entry.name}"
                }
            }

            val manifestEntry = zip.getEntry(BackupFormat.MANIFEST_ENTRY)
                ?: error("Backup manifest is missing.")

            require(manifestEntry.size in 1..BackupSafetyLimits.MAX_MANIFEST_BYTES) {
                "Backup manifest is invalid or exceeds max size."
            }

            val manifest = zip.getInputStream(manifestEntry).use {
                manifestCodec.read(it)
            }

            manifestCodec.validateCompatibility(manifest)
            validateEntrySet(zip, manifest, names)

            val expected = manifest.entries.associateBy { it.name }
            var totalJson = 0L

            expected.values.forEachIndexed { index, expectedEntry ->
                val actual = zip.getEntry(expectedEntry.name)
                    ?: error("Backup entry ${expectedEntry.name} is missing.")

                require(actual.size == expectedEntry.uncompressedBytes) {
                    "Backup file size check failed for ${expectedEntry.name}."
                }

                if (expectedEntry.name.startsWith("data/") || expectedEntry.name.startsWith("settings/")) {
                    require(actual.size <= BackupSafetyLimits.MAX_SINGLE_JSON_BYTES) {
                        "JSON entry ${expectedEntry.name} exceeds single file safety limit."
                    }
                    totalJson += actual.size
                    require(totalJson <= BackupSafetyLimits.MAX_TOTAL_JSON_BYTES) {
                        "Total JSON size exceeds safety limit."
                    }
                }

                validateCompressionRatio(actual)

                val digest = sha256(zip.getInputStream(actual))
                require(digest.equals(expectedEntry.sha256, ignoreCase = true)) {
                    "Backup checksum validation failed for ${expectedEntry.name}."
                }

                onProgress(((index + 1).toFloat() / expected.size.coerceAtLeast(1) * 80f).toInt())
            }

            // Referential integrity & structure
            structureValidator.validate(zip, manifest)

            val totalMedia = manifest.media.sumOf { it.uncompressedBytes }
            onProgress(100)

            return BackupInspection(
                sessionId = sessionId,
                encrypted = encrypted,
                manifest = manifest,
                totalMediaBytes = totalMedia,
                validationWarnings = manifest.warnings
            )
        }
    }

    private fun validateEntrySet(
        zip: ZipFile,
        manifest: BackupManifest,
        archiveNames: Set<String>
    ) {
        val manifestNames = manifest.entries.map { it.name }.toSet()
        require(manifestNames.size == manifest.entries.size) {
            "Backup manifest has duplicate entries."
        }

        manifestNames.forEach { name ->
            BackupZipSafety.requireSafeName(name)
            val allowed = name in BackupFormat.DATA_ENTRIES || name.startsWith(BackupFormat.MEDIA_PREFIX)
            require(allowed) {
                "Backup contains an unsupported entry: $name"
            }
        }

        require(manifest.media.size <= BackupSafetyLimits.MAX_MEDIA_ENTRIES) {
            "Media entry count exceeds safety limit."
        }

        manifest.media.forEach { media ->
            require(media.entryName.startsWith(BackupFormat.MEDIA_PREFIX)) {
                "Invalid media entry name: ${media.entryName}"
            }
            require(media.entryName in manifestNames) {
                "Media entry not found in manifest entries: ${media.entryName}"
            }
            require(media.uncompressedBytes >= 0L) {
                "Negative media size for ${media.entryName}"
            }
        }

        val expectedArchive = manifestNames + BackupFormat.MANIFEST_ENTRY
        require(archiveNames == expectedArchive) {
            "Backup archive contains unexpected or missing files."
        }
    }

    private fun validateCompressionRatio(entry: ZipEntry) {
        val compressed = entry.compressedSize
        val uncompressed = entry.size
        if (compressed > 0L && uncompressed > 10L * 1024L * 1024L) {
            val ratio = uncompressed.toDouble() / compressed.toDouble()
            require(ratio <= BackupSafetyLimits.MAX_COMPRESSION_RATIO) {
                "Backup compression ratio is unsafe for ${entry.name}."
            }
        }
    }

    private fun sha256(input: InputStream): String = input.buffered(64 * 1024).use { stream ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            if (read > 0) {
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
