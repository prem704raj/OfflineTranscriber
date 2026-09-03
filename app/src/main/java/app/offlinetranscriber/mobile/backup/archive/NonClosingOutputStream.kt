package app.offlinetranscriber.mobile.backup.archive

import java.io.FilterOutputStream
import java.io.OutputStream

class NonClosingOutputStream(
    output: OutputStream
) : FilterOutputStream(output) {

    override fun close() {
        flush()
    }
}
