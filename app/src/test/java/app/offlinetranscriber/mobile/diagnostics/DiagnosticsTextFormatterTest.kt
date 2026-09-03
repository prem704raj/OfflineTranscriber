package app.offlinetranscriber.mobile.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTextFormatterTest {

    @Test
    fun formatIncludesOperationalMetadata() {
        val diag = AppDiagnostics(
            appVersionName = "1.0.0",
            appVersionCode = 1L,
            androidRelease = "15",
            apiLevel = 35,
            totalRamBytes = 8L * 1024 * 1024 * 1024,
            cpuCores = 8,
            activeModelLabel = "Base (Balanced)",
            installedModelLabels = listOf("Base (Balanced)", "Tiny (Fast)"),
            languageLabel = "English",
            databaseBytes = 1024 * 50,
            transcriptCount = 12,
            segmentCount = 120,
            bookmarkCount = 5,
            collectionCount = 2,
            studyPackCount = 3,
            askConversationCount = 4,
            askMessageCount = 10,
            meetingPackCount = 2,
            meetingActionCount = 6,
            queue = QueueDiagnostics(
                queued = 1,
                processing = 0,
                completed = 11,
                failed = 0,
                cancelled = 0
            ),
            recordingsBytes = 1024 * 1024 * 5,
            extractedAudioBytes = 1024 * 1024 * 10,
            temporaryBytes = 1024 * 512,
            modelsBytes = 1024 * 1024 * 200,
            freeInternalBytes = 1024L * 1024 * 1024 * 25,
            entitlement = "PRO",
            nanoStatus = "AVAILABLE"
        )

        val text = DiagnosticsTextFormatter.format(diag)

        assertTrue(text.contains("1.0.0 (1)"))
        assertTrue(text.contains("Base (Balanced)"))
        assertTrue(text.contains("Transcripts: 12"))
        assertTrue(text.contains("Segments: 120"))
        assertTrue(text.contains("Entitlement: PRO"))
        assertTrue(text.contains("On-device study AI: AVAILABLE"))
        assertTrue(text.contains("Privacy note:"))
    }

    @Test
    fun releaseLoggerSanitizesSensitiveFields() {
        val raw = "User action with purchaseToken: secret123 and uri: content://media/123 transcript: private text"
        val sanitized = ReleaseLogger.sanitize(raw)

        assertFalse(sanitized.contains("secret123"))
        assertFalse(sanitized.contains("content://media/123"))
        assertFalse(sanitized.contains("private text"))
        assertTrue(sanitized.contains("[redacted]"))
    }
}
