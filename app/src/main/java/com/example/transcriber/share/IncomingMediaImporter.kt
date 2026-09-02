package com.example.transcriber.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.transcriber.performance.CapacityResult
import com.example.transcriber.performance.StorageCapacityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class IncomingMediaImporter(
    private val context: Context
) {

    private val storageCapacityChecker = StorageCapacityChecker(context)

    suspend fun import(
        item: IncomingShareItem,
        incomingIntentFlags: Int
    ): ImportedShareItem = withContext(Dispatchers.IO) {
        val name = displayName(item.uri)

        if (
            canPersist(incomingIntentFlags) &&
            tryPersist(item.uri, incomingIntentFlags)
        ) {
            return@withContext ImportedShareItem(
                usableUri = item.uri,
                displayName = name,
                kind = item.kind,
                appOwnedCopy = false
            )
        }

        val estimatedSize = querySize(item.uri) ?: (250L * 1024L * 1024L)

        when (storageCapacityChecker.checkInternal(estimatedSize)) {
            CapacityResult.Enough -> Unit
            is CapacityResult.NotEnough -> {
                error("Not enough storage to import this file. Free up space and try again.")
            }
        }

        val dir = File(
            context.filesDir,
            "shared_imports"
        ).apply {
            mkdirs()
        }

        val extension = extensionFor(item.mimeType, name)
        val target = File(dir, "shared_${UUID.randomUUID()}$extension")

        try {
            val input = context.contentResolver.openInputStream(item.uri)
                ?: error("Unable to read shared media.")

            input.use { source ->
                target.outputStream().buffered().use { destination ->
                    source.buffered().copyTo(destination, 128 * 1024)
                }
            }

            if (!target.exists() || target.length() <= 0L) {
                target.delete()
                error("Shared media copy failed.")
            }

            ImportedShareItem(
                usableUri = Uri.fromFile(target),
                displayName = name,
                kind = item.kind,
                appOwnedCopy = true
            )
        } catch (t: Throwable) {
            target.delete()
            throw t
        }
    }

    private fun canPersist(flags: Int): Boolean =
        (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0

    private fun tryPersist(uri: Uri, flags: Int): Boolean {
        if (uri.scheme != "content") return false

        val takeFlags = flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        return runCatching {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        }.isSuccess
    }

    private fun querySize(uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst()) {
                    cursor.getLong(index).takeIf { it > 0L }
                } else null
            }
        }.getOrNull()
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return sanitizeName(it) }
            }
        }

        return when {
            uri.lastPathSegment.isNullOrBlank() -> "Shared media"
            else -> sanitizeName(uri.lastPathSegment!!)
        }
    }

    private fun sanitizeName(value: String): String =
        value
            .replace(Regex("""[\r\n\t]"""), " ")
            .take(120)
            .ifBlank { "Shared media" }

    private fun extensionFor(mime: String, displayName: String): String {
        val existing = displayName
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.matches(Regex("""[a-z0-9]{1,6}""")) }

        val ext = existing ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)

        return ext
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            .orEmpty()
    }
}
