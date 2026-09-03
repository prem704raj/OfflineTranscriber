package app.offlinetranscriber.mobile.backup.data

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptBackupRow(
    val oldId: Long,
    val title: String,
    val audioFileName: String,
    val audioDurationMs: Long,
    val createdAt: Long,
    val fullText: String,
    val segmentsJson: String,
    val modelUsed: String,
    val audioUriString: String?,
    val sourceUri: String,
    val mediaType: String,
    val contentFingerprint: String
)

@Serializable
data class TranscriptSegmentBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Serializable
data class BookmarkBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val segmentOldId: Long,
    val note: String? = null,
    val createdAt: Long
)

@Serializable
data class CollectionBackupRow(
    val oldId: Long,
    val name: String,
    val createdAt: Long
)

@Serializable
data class CollectionMembershipBackupRow(
    val collectionOldId: Long,
    val transcriptOldId: Long
)

@Serializable
data class StudyPackBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val engine: String,
    val generatedAt: Long
)

@Serializable
data class StudyKeyPointBackupRow(
    val oldId: Long,
    val studyPackOldId: Long,
    val position: Int,
    val text: String
)

@Serializable
data class StudyChapterBackupRow(
    val oldId: Long,
    val studyPackOldId: Long,
    val position: Int,
    val title: String,
    val startMs: Long,
    val summary: String
)

@Serializable
data class FlashcardBackupRow(
    val oldId: Long,
    val studyPackOldId: Long,
    val position: Int,
    val front: String,
    val back: String,
    val status: String
)

@Serializable
data class QuizQuestionBackupRow(
    val oldId: Long,
    val studyPackOldId: Long,
    val position: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String
)

@Serializable
data class AskConversationBackupRow(
    val oldId: Long,
    val scope: String,
    val transcriptOldId: Long?,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class AskMessageBackupRow(
    val oldId: Long,
    val conversationOldId: Long,
    val role: String,
    val text: String,
    val engine: String?,
    val insufficientEvidence: Boolean,
    val createdAt: Long
)

@Serializable
data class AskCitationBackupRow(
    val oldId: Long,
    val messageOldId: Long,
    val segmentOldId: Long,
    val position: Int
)

@Serializable
data class MeetingPackBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val summary: String,
    val engine: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class MeetingSummaryCitationBackupRow(
    val meetingPackOldId: Long,
    val segmentOldId: Long,
    val position: Int
)

@Serializable
data class MeetingActionBackupRow(
    val oldId: Long,
    val meetingPackOldId: Long,
    val text: String,
    val assignee: String,
    val dueText: String,
    val sourceSegmentOldId: Long?,
    val startMs: Long,
    val status: String,
    val manuallyEdited: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class MeetingDecisionBackupRow(
    val oldId: Long,
    val meetingPackOldId: Long,
    val text: String,
    val sourceSegmentOldId: Long?,
    val startMs: Long,
    val createdAt: Long
)

@Serializable
data class MeetingQuestionBackupRow(
    val oldId: Long,
    val meetingPackOldId: Long,
    val text: String,
    val sourceSegmentOldId: Long?,
    val startMs: Long,
    val createdAt: Long
)

@Serializable
data class MeetingTopicBackupRow(
    val oldId: Long,
    val meetingPackOldId: Long,
    val title: String,
    val sourceSegmentOldId: Long?,
    val startMs: Long,
    val createdAt: Long
)

@Serializable
data class SpeakerClusterBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val speakerIndex: Int,
    val customName: String,
    val userNamed: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SpeakerTurnBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val speakerClusterOldId: Long,
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class SegmentSpeakerAssignmentBackupRow(
    val segmentOldId: Long,
    val speakerClusterOldId: Long,
    val overlapRatio: Float
)

@Serializable
data class SpeakerDiarizationRunBackupRow(
    val oldId: Long,
    val transcriptOldId: Long,
    val status: String,
    val progress: Int,
    val speakerCountMode: String,
    val requestedSpeakerCount: Int?,
    val detectedSpeakerCount: Int,
    val engineVersion: String,
    val segmentationModelId: String,
    val embeddingModelId: String,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class MediaLinkBackupRow(
    val transcriptOldId: Long,
    val mediaId: String
)

@Serializable
data class PortableSettingsBackup(
    val languageCode: String? = null,
    val onboardingComplete: Boolean? = null
)
