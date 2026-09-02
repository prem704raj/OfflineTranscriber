package com.example.transcriber.share

import android.net.Uri

data class ImportedShareItem(
    val usableUri: Uri,
    val displayName: String,
    val kind: IncomingMediaKind,
    val appOwnedCopy: Boolean
)
