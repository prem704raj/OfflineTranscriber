package app.offlinetranscriber.mobile.diagnostics

import java.util.Locale

object DiagnosticsTextFormatter {

    fun format(
        value: AppDiagnostics
    ): String = buildString {
        appendLine("Offline Transcriber diagnostics")
        appendLine("Generated: ${System.currentTimeMillis()}")
        appendLine()

        appendLine("App version: ${value.appVersionName} (${value.appVersionCode})")
        appendLine("Android: ${value.androidRelease} / API ${value.apiLevel}")
        appendLine("RAM: ${formatBytes(value.totalRamBytes)}")
        appendLine("CPU cores: ${value.cpuCores}")
        appendLine()

        appendLine("Active model: ${value.activeModelLabel}")
        appendLine("Installed models: ${value.installedModelLabels.joinToString().ifBlank { "None" }}")
        appendLine("Language: ${value.languageLabel}")
        appendLine("Entitlement: ${value.entitlement}")
        appendLine("On-device study AI: ${value.nanoStatus}")
        appendLine()

        appendLine("Transcripts: ${value.transcriptCount}")
        appendLine("Segments: ${value.segmentCount}")
        appendLine("Bookmarks: ${value.bookmarkCount}")
        appendLine("Collections: ${value.collectionCount}")
        appendLine("Study packs: ${value.studyPackCount}")
        appendLine("Ask conversations: ${value.askConversationCount}")
        appendLine("Ask messages: ${value.askMessageCount}")
        appendLine("Meeting packs: ${value.meetingPackCount}")
        appendLine("Meeting actions: ${value.meetingActionCount}")
        appendLine("Diarized transcripts: ${value.diarizedTranscriptCount}")
        appendLine("Speaker clusters: ${value.speakerClusterCount}")
        appendLine("Speaker turns: ${value.speakerTurnCount}")
        appendLine()

        appendLine("Queue queued: ${value.queue.queued}")
        appendLine("Queue processing: ${value.queue.processing}")
        appendLine("Queue completed: ${value.queue.completed}")
        appendLine("Queue failed: ${value.queue.failed}")
        appendLine("Queue cancelled: ${value.queue.cancelled}")
        appendLine()

        appendLine("Database: ${formatBytes(value.databaseBytes)}")
        appendLine("Models: ${formatBytes(value.modelsBytes)}")
        appendLine("Recordings: ${formatBytes(value.recordingsBytes)}")
        appendLine("Extracted media: ${formatBytes(value.extractedAudioBytes)}")
        appendLine("Temporary: ${formatBytes(value.temporaryBytes)}")
        appendLine("Free internal storage: ${formatBytes(value.freeInternalBytes)}")
        appendLine()

        appendLine("Privacy note: This diagnostic report intentionally excludes transcript text, source filenames, file URIs, purchase tokens, prompts and media content.")
    }

    private fun formatBytes(
        bytes: Long
    ): String {
        if (bytes < 1024L) {
            return "$bytes B"
        }

        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = -1

        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }

        return String.format(
            Locale.US,
            "%.1f %s",
            value,
            units[index]
        )
    }
}
