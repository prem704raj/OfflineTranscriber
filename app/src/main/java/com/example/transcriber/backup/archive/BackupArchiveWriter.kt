package com.example.transcriber.backup.archive

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.transcriber.backup.BackupOptions
import com.example.transcriber.backup.BackupProgress
import com.example.transcriber.backup.crypto.BackupCrypto
import com.example.transcriber.backup.format.BackupManifest
import com.example.transcriber.backup.format.BackupManifestEntry
import com.example.transcriber.backup.format.BackupMediaManifestEntry
import com.example.transcriber.backup.data.BackupSectionExporter
import com.example.transcriber.backup.data.BookmarkBackupExporter
import com.example.transcriber.backup.data.CollectionBackupExporter
import com.example.transcriber.backup.data.CollectionMembershipBackupExporter
import com.example.transcriber.backup.data.MediaLinkBackupExporter
import com.example.transcriber.backup.data.MediaLinkBackupRow
import com.example.transcriber.backup.data.MeetingActionBackupExporter
import com.example.transcriber.backup.data.MeetingDecisionBackupExporter
import com.example.transcriber.backup.data.MeetingPackBackupExporter
import com.example.transcriber.backup.data.MeetingQuestionBackupExporter
import com.example.transcriber.backup.data.MeetingSummaryCitationBackupExporter
import com.example.transcriber.backup.data.MeetingTopicBackupExporter
import com.example.transcriber.backup.data.PortableSettingsExporter
import com.example.transcriber.backup.data.QuizQuestionBackupExporter
import com.example.transcriber.backup.data.SegmentSpeakerAssignmentBackupExporter
import com.example.transcriber.backup.data.SpeakerClusterBackupExporter
import com.example.transcriber.backup.data.SpeakerDiarizationRunBackupExporter
import com.example.transcriber.backup.data.SpeakerTurnBackupExporter
import com.example.transcriber.backup.data.StudyChapterBackupExporter
import com.example.transcriber.backup.data.StudyKeyPointBackupExporter
import com.example.transcriber.backup.data.StudyPackBackupExporter
import com.example.transcriber.backup.data.TranscriptBackupExporter
import com.example.transcriber.backup.data.TranscriptSegmentsBackupExporter
import com.example.transcriber.backup.format.BackupFormat
import com.example.transcriber.backup.format.BackupManifestCodec
import com.example.transcriber.backup.media.BackupMediaPlanner
import com.example.transcriber.backup.restore.BackupArchiveValidator
import com.example.transcriber.backup.restore.BackupPayloadMaterializer
import com.example.transcriber.backup.restore.RestoreWorkspace
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.settings.AppSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

data class BackupWriteResult(
    val backupId: String,
    val uncompressedBytes: Long,
    val mediaFilesCount: Int,
    val warnings: List<String>
)

class BackupArchiveWriter(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: AppSettingsRepository,
    private val manifestCodec: BackupManifestCodec,
    private val crypto: BackupCrypto,
    private val workspace: RestoreWorkspace,
    private val payloadMaterializer: BackupPayloadMaterializer,
    private val archiveValidator: BackupArchiveValidator
) {

    suspend fun write(
        destination: Uri,
        options: BackupOptions,
        password: CharArray?,
        onProgress: suspend (BackupProgress) -> Unit = {},
        cancelled: () -> Boolean = { false }
    ): BackupWriteResult = withContext(Dispatchers.IO) {
        val backupId = UUID.randomUUID().toString()
        val warnings = mutableListOf<String>()
        val mediaLinks = mutableListOf<MediaLinkBackupRow>()
        val mediaManifestEntries = mutableListOf<com.example.transcriber.backup.format.BackupMediaManifestEntry>()
        val manifestEntries = mutableListOf<com.example.transcriber.backup.format.BackupManifestEntry>()
        val sectionCounts = linkedMapOf<String, Long>()

        val rawOutput = context.contentResolver.openOutputStream(destination, "w")
            ?: error("Unable to open backup destination: $destination")

        var totalCopiedBytes = 0L

        try {
            val payloadOutput: OutputStream = if (options.passwordProtected) {
                val secret = password ?: error("Backup password is required for encrypted backup.")
                crypto.createEncryptingStream(rawOutput, secret)
            } else {
                rawOutput
            }

            ZipOutputStream(BufferedOutputStream(payloadOutput, 64 * 1024)).use { zip ->
                zip.setLevel(6)
                val entryWriter = ZipEntryDigestWriter(zip)
                val dao = database.backupDao()

                onProgress(BackupProgress("Writing transcripts", 5))

                val dbExporters = listOf<BackupSectionExporter>(
                    TranscriptBackupExporter(dao),
                    TranscriptSegmentsBackupExporter(dao),
                    BookmarkBackupExporter(dao),
                    CollectionBackupExporter(dao),
                    CollectionMembershipBackupExporter(dao),
                    StudyPackBackupExporter(dao),
                    StudyKeyPointBackupExporter(dao),
                    StudyChapterBackupExporter(dao),
                    com.example.transcriber.backup.data.FlashcardBackupExporter(dao),
                    QuizQuestionBackupExporter(dao),
                    com.example.transcriber.backup.data.AskConversationBackupExporter(dao),
                    com.example.transcriber.backup.data.AskMessageBackupExporter(dao),
                    com.example.transcriber.backup.data.AskCitationBackupExporter(dao),
                    MeetingPackBackupExporter(dao),
                    MeetingSummaryCitationBackupExporter(dao),
                    MeetingActionBackupExporter(dao),
                    MeetingDecisionBackupExporter(dao),
                    MeetingQuestionBackupExporter(dao),
                    MeetingTopicBackupExporter(dao),
                    SpeakerClusterBackupExporter(dao),
                    SpeakerTurnBackupExporter(dao),
                    SegmentSpeakerAssignmentBackupExporter(dao),
                    SpeakerDiarizationRunBackupExporter(dao)
                )

                val mediaPlanner = BackupMediaPlanner(context, dao)
                val mediaPlan = mediaPlanner.plan(options.includeMedia)

                // 1. Transactional DB snapshot export
                database.withTransaction {
                    dbExporters.forEachIndexed { index, exporter ->
                        if (cancelled()) throw CancellationException("Backup cancelled by user")
                        coroutineContext.ensureActive()

                        val manifestEntry = entryWriter.write(exporter.entryName) { stream ->
                            val result = exporter.export(stream)
                            sectionCounts[exporter.entryName] = result.rowCount
                        }
                        manifestEntries.add(manifestEntry)
                        totalCopiedBytes += manifestEntry.uncompressedBytes

                        val pct = 5 + ((index + 1).toFloat() / dbExporters.size * 35f).toInt()
                        onProgress(BackupProgress("Writing app data", pct))
                    }
                }

                // 2. Settings export
                if (options.includeSettings) {
                    val settingsExporter = PortableSettingsExporter(settingsRepository)
                    val manifestEntry = entryWriter.write(settingsExporter.entryName) { stream ->
                        val result = settingsExporter.export(stream)
                        sectionCounts[settingsExporter.entryName] = result.rowCount
                    }
                    manifestEntries.add(manifestEntry)
                    totalCopiedBytes += manifestEntry.uncompressedBytes
                }

                // 3. Media export
                if (options.includeMedia && mediaPlan.isNotEmpty()) {
                    onProgress(BackupProgress("Copying source media", 45))
                    mediaPlan.forEachIndexed { index, mediaSource ->
                        if (cancelled()) throw CancellationException("Backup cancelled by user")
                        coroutineContext.ensureActive()

                        val entryName = "media/${mediaSource.mediaId}.${mediaSource.extension}"
                        var mediaCopied = false

                        val manifestEntry = runCatching {
                            entryWriter.write(entryName) { stream ->
                                context.contentResolver.openInputStream(mediaSource.uri)?.use { input ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        val read = input.read(buffer)
                                        if (read < 0) break
                                        if (read > 0) stream.write(buffer, 0, read)
                                    }
                                } ?: error("Unable to open source media: ${mediaSource.uri}")
                            }
                        }.getOrElse { err ->
                            warnings.add("1 media file could not be read and was omitted.")
                            null
                        }

                        if (manifestEntry != null) {
                            manifestEntries.add(manifestEntry)
                            mediaManifestEntries.add(
                                com.example.transcriber.backup.format.BackupMediaManifestEntry(
                                    mediaId = mediaSource.mediaId,
                                    transcriptOldId = mediaSource.transcriptOldId,
                                    role = "SOURCE_MEDIA",
                                    entryName = entryName,
                                    displayName = mediaSource.displayName,
                                    mimeType = mediaSource.mimeType,
                                    uncompressedBytes = manifestEntry.uncompressedBytes,
                                    sha256 = manifestEntry.sha256
                                )
                            )
                            mediaLinks.add(
                                MediaLinkBackupRow(
                                    transcriptOldId = mediaSource.transcriptOldId,
                                    mediaId = mediaSource.mediaId
                                )
                            )
                            totalCopiedBytes += manifestEntry.uncompressedBytes
                        }

                        val pct = 45 + ((index + 1).toFloat() / mediaPlan.size * 35f).toInt()
                        onProgress(BackupProgress("Copying source media", pct))
                    }
                }

                // 4. Media links export
                val mediaLinkExporter = MediaLinkBackupExporter(mediaLinks)
                val mediaLinkManifestEntry = entryWriter.write(mediaLinkExporter.entryName) { stream ->
                    val result = mediaLinkExporter.export(stream)
                    sectionCounts[mediaLinkExporter.entryName] = result.rowCount
                }
                manifestEntries.add(mediaLinkManifestEntry)
                totalCopiedBytes += mediaLinkManifestEntry.uncompressedBytes

                // 5. Manifest export LAST
                onProgress(BackupProgress("Finalizing manifest", 82))
                val packageInfo = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }.getOrNull()

                val versionName = packageInfo?.versionName ?: "1.2.0"
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: 1L
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: 1L
                }

                val manifest = com.example.transcriber.backup.format.BackupManifest(
                    product = BackupFormat.PRODUCT,
                    formatVersion = BackupFormat.FORMAT_VERSION,
                    backupId = backupId,
                    createdAtEpochMs = System.currentTimeMillis(),
                    appVersionName = versionName,
                    appVersionCode = versionCode,
                    sourceRoomSchemaVersion = 9,
                    includesSettings = options.includeSettings,
                    includesMedia = options.includeMedia && mediaManifestEntries.isNotEmpty(),
                    sectionCounts = sectionCounts,
                    entries = manifestEntries,
                    media = mediaManifestEntries,
                    warnings = warnings
                )

                entryWriter.write(BackupFormat.MANIFEST_ENTRY) { stream ->
                    manifestCodec.write(manifest, stream)
                }

                zip.flush()
            }

            // 6. Backup Self-Validation
            onProgress(BackupProgress("Verifying backup", 88))
            val verificationSessionId = "verify_$backupId"
            try {
                val materialized = payloadMaterializer.materialize(
                    sessionId = verificationSessionId,
                    sourceUri = destination,
                    password = password
                )
                archiveValidator.validate(
                    sessionId = verificationSessionId,
                    file = materialized.zipFile,
                    encrypted = materialized.encrypted
                ) { verifyProgress ->
                    val pct = 88 + (verifyProgress * 0.11f).toInt()
                    // Throttled progress inside validation
                }
            } finally {
                workspace.cleanup(verificationSessionId)
            }

            onProgress(BackupProgress("Complete", 100))

            BackupWriteResult(
                backupId = backupId,
                uncompressedBytes = totalCopiedBytes,
                mediaFilesCount = mediaManifestEntries.size,
                warnings = warnings
            )
        } finally {
            password?.fill('\u0000')
        }
    }
}
