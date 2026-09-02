package com.example.transcriber.video

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Context.displayNameFor(uri: Uri): String {
    if (uri.scheme == "content") {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
    }

    return uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
        ?: "Video"
}

fun String.filenameWithoutExtension(): String {
    return substringBeforeLast('.', missingDelimiterValue = this)
        .ifBlank { "Video transcript" }
}
