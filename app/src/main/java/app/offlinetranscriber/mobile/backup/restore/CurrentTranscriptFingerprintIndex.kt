package app.offlinetranscriber.mobile.backup.restore

import app.offlinetranscriber.mobile.backup.fingerprint.FingerprintSegment
import app.offlinetranscriber.mobile.backup.fingerprint.TranscriptFingerprint
import app.offlinetranscriber.mobile.data.database.BackupDao
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity

class CurrentTranscriptFingerprintIndex(
    private val dao: BackupDao
) {

    data class ExistingTranscriptMatch(
        val transcriptId: Long,
        val segments: List<TranscriptSegmentEntity>
    )

    suspend fun buildIndex(): Map<String, ExistingTranscriptMatch> {
        val index = mutableMapOf<String, ExistingTranscriptMatch>()
        var afterId = Long.MIN_VALUE

        while (true) {
            val page = dao.backupTranscriptsAfter(afterId, 250)
            if (page.isEmpty()) break

            for (t in page) {
                val segments = dao.getSegmentsForTranscript(t.id)
                val fingerprint = TranscriptFingerprint.calculate(
                    durationMs = t.audioDurationMs,
                    mediaType = t.mediaType,
                    segments = segments.asSequence().map {
                        FingerprintSegment(it.startMs, it.endMs, it.text)
                    }
                )

                if (!index.containsKey(fingerprint)) {
                    index[fingerprint] = ExistingTranscriptMatch(
                        transcriptId = t.id,
                        segments = segments
                    )
                }
                afterId = t.id
            }
        }

        return index
    }
}
