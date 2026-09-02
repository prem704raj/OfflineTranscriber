package com.example.transcriber.backup.format

object BackupFormat {

    const val PRODUCT = "offline-transcriber"

    const val FORMAT_VERSION = 1

    const val FILE_EXTENSION = "otbackup"

    const val MIME = "application/octet-stream"

    const val MANIFEST_ENTRY = "manifest.json"

    val DATA_ENTRIES = setOf(
        "data/transcripts.json",
        "data/transcript_segments.json",
        "data/bookmarks.json",
        "data/collections.json",
        "data/collection_memberships.json",

        "data/study_packs.json",
        "data/study_key_points.json",
        "data/study_chapters.json",
        "data/flashcards.json",
        "data/quiz_questions.json",

        "data/ask_conversations.json",
        "data/ask_messages.json",
        "data/ask_citations.json",

        "data/meeting_packs.json",
        "data/meeting_summary_citations.json",
        "data/meeting_actions.json",
        "data/meeting_decisions.json",
        "data/meeting_questions.json",
        "data/meeting_topics.json",

        "data/speaker_clusters.json",
        "data/speaker_turns.json",
        "data/segment_speaker_assignments.json",
        "data/speaker_diarization_runs.json",

        "data/media_links.json",

        "settings/portable_settings.json"
    )

    const val MEDIA_PREFIX = "media/"
}
