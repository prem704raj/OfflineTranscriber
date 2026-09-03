package app.offlinetranscriber.mobile.media

import android.content.Context
import android.net.Uri
import java.io.File

sealed interface MediaAvailability {
    data object Available : MediaAvailability
    data class Unavailable(val reason: String) : MediaAvailability
}

class MediaAvailabilityChecker(
    private val context: Context
) {

    fun check(
        uriString: String?
    ): MediaAvailability {
        if (uriString.isNullOrBlank()) {
            return MediaAvailability.Unavailable("Source file is unavailable.")
        }

        val uri = runCatching {
            Uri.parse(uriString)
        }.getOrElse {
            return MediaAvailability.Unavailable("Source file is unavailable.")
        }

        return when (uri.scheme) {
            "content" -> checkContent(uri)
            "file" -> checkFile(uri)
            "asset" -> MediaAvailability.Available
            else -> {
                val file = File(uriString)
                if (file.exists() && file.isFile && file.length() > 0L) {
                    MediaAvailability.Available
                } else {
                    MediaAvailability.Unavailable("Source file is unavailable.")
                }
            }
        }
    }

    private fun checkContent(
        uri: Uri
    ): MediaAvailability {
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                if (pfd.fileDescriptor.valid()) {
                    MediaAvailability.Available
                } else {
                    MediaAvailability.Unavailable("Source file is unavailable.")
                }
            } ?: MediaAvailability.Unavailable("Source file is unavailable.")
        }.getOrElse {
            MediaAvailability.Unavailable(
                "Source permission was removed or the file was moved."
            )
        }
    }

    private fun checkFile(
        uri: Uri
    ): MediaAvailability {
        val path = uri.path ?: return MediaAvailability.Unavailable("Source file is unavailable.")
        val file = File(path)
        return if (file.exists() && file.isFile && file.length() > 0L) {
            MediaAvailability.Available
        } else {
            MediaAvailability.Unavailable("Source file is unavailable.")
        }
    }
}
