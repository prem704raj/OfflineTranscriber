package com.example.transcriber.backup.restore

import com.example.transcriber.backup.RestoreMode

data class RestorePlan(
    val mode: RestoreMode,
    val duplicateTranscriptMapping: Map<Long, Long>, // backup transcript oldId -> current local transcript ID
    val duplicateSegmentMapping: Map<Long, Long>, // backup segment oldId -> current local segment ID
    val newTranscriptOldIds: Set<Long>,
    val requiredMediaTranscriptOldIds: Set<Long>,
    val skippedStudyTranscriptOldIds: Set<Long>,
    val skippedMeetingTranscriptOldIds: Set<Long>,
    val skippedSpeakerTranscriptOldIds: Set<Long>
)
