package com.example.transcriber.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

class IncomingShareParser(
    private val context: Context
) {
    companion object {
        const val MAX_ITEMS = 20
    }

    fun parse(
        intent: Intent
    ): List<IncomingShareItem> {
        if (
            intent.action != Intent.ACTION_SEND &&
            intent.action != Intent.ACTION_SEND_MULTIPLE
        ) {
            return emptyList()
        }

        val streams = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(getSingleStream(intent))
            Intent.ACTION_SEND_MULTIPLE -> getMultipleStreams(intent)
            else -> emptyList()
        }
            .distinct()
            .take(MAX_ITEMS)

        return streams.mapNotNull { uri ->
            val mime = context.contentResolver.getType(uri)
                ?: intent.type
                ?: return@mapNotNull null

            val normalized = mime.lowercase()

            val kind = when {
                normalized.startsWith("audio/") -> IncomingMediaKind.AUDIO
                normalized.startsWith("video/") -> IncomingMediaKind.VIDEO
                else -> return@mapNotNull null
            }

            IncomingShareItem(
                uri = uri,
                mimeType = normalized,
                kind = kind
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getSingleStream(
        intent: Intent
    ): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }

    @Suppress("DEPRECATION")
    private fun getMultipleStreams(
        intent: Intent
    ): List<Uri> = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }
}
