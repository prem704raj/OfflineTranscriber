package app.offlinetranscriber.mobile.export.render.docx

object XmlEscaper {

    fun escape(text: String): String {
        return buildString(text.length + 16) {
            for (ch in text) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> {
                        if (ch.code in 0x20..0xD7FF || ch == '\t' || ch == '\n' || ch == '\r' || ch.code in 0xE000..0xFFFD) {
                            append(ch)
                        }
                    }
                }
            }
        }
    }
}
