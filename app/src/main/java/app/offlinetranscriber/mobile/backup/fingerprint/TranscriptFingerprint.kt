package app.offlinetranscriber.mobile.backup.fingerprint

import java.security.MessageDigest

data class FingerprintSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object TranscriptFingerprint {

    fun calculate(
        durationMs: Long,
        mediaType: String,
        segments: Sequence<FingerprintSegment>
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")

        fun add(value: String) {
            digest.update(value.toByteArray(Charsets.UTF_8))
            digest.update(0)
        }

        add("offline-transcriber-transcript-fingerprint-v1")
        add(durationMs.toString())
        add(mediaType)

        segments.forEach { segment ->
            add(segment.startMs.toString())
            add(segment.endMs.toString())
            add(segment.text.replace(Regex("""\s+"""), " ").trim())
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
