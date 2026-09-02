package com.example.transcriber.backup.archive

import java.io.FilterOutputStream
import java.io.OutputStream

class NonClosingOutputStream(
    output: OutputStream
) : FilterOutputStream(output) {

    override fun close() {
        flush()
    }
}
