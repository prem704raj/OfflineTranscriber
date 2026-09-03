package app.offlinetranscriber.mobile

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeNavKey : NavKey

@Serializable
data object SearchNavKey : NavKey

@Serializable
data object LibraryNavKey : NavKey

@Serializable
data object QueueNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey

@Serializable
data object ModelManagerNavKey : NavKey

@Serializable
data object OnboardingNavKey : NavKey

@Serializable
data class PaywallNavKey(val reason: String? = null) : NavKey

@Serializable
data object PrivacyDataNavKey : NavKey

@Serializable
data object DiagnosticsNavKey : NavKey

@Serializable
data object AboutNavKey : NavKey

@Serializable
data class CollectionDetailNavKey(val collectionId: Long) : NavKey

@Serializable
data class DetailNavKey(val transcriptId: Long, val seekMs: Long = 0L) : NavKey

@Serializable
data class VideoPrepareNavKey(val videoUriString: String) : NavKey

@Serializable
data class SubtitleNavKey(val transcriptId: Long, val seekMs: Long = 0L) : NavKey

@Serializable
data class StudyNavKey(val transcriptId: Long) : NavKey

@Serializable
data class AskNavKey(val scope: String, val transcriptId: Long? = null) : NavKey

@Serializable
data class MeetingNavKey(val transcriptId: Long) : NavKey

@Serializable
data class SpeakerNavKey(val transcriptId: Long) : NavKey

@Serializable
data class ExportNavKey(val contentType: String, val sourceId: Long) : NavKey

@Serializable
data object BackupRestoreNavKey : NavKey

@Serializable
data object CreateBackupNavKey : NavKey

@Serializable
data object RestorePreviewNavKey : NavKey

@Serializable
data object RecordNavKey : NavKey

