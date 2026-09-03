package app.offlinetranscriber.mobile.backup.restore

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import androidx.room.withTransaction
import app.offlinetranscriber.mobile.backup.RestoreMode
import app.offlinetranscriber.mobile.backup.RestoreResult
import app.offlinetranscriber.mobile.backup.fingerprint.FingerprintSegment
import app.offlinetranscriber.mobile.backup.fingerprint.TranscriptFingerprint
import app.offlinetranscriber.mobile.backup.settings.PortableSettingsRestorer
import app.offlinetranscriber.mobile.data.database.AppDatabase
import app.offlinetranscriber.mobile.data.model.AskCitationEntity
import app.offlinetranscriber.mobile.data.model.AskConversationEntity
import app.offlinetranscriber.mobile.data.model.AskMessageEntity
import app.offlinetranscriber.mobile.data.model.BookmarkEntity
import app.offlinetranscriber.mobile.data.model.CollectionEntity
import app.offlinetranscriber.mobile.data.model.FlashcardEntity
import app.offlinetranscriber.mobile.data.model.MeetingActionEntity
import app.offlinetranscriber.mobile.data.model.MeetingDecisionEntity
import app.offlinetranscriber.mobile.data.model.MeetingPackEntity
import app.offlinetranscriber.mobile.data.model.MeetingQuestionEntity
import app.offlinetranscriber.mobile.data.model.MeetingSummaryCitationEntity
import app.offlinetranscriber.mobile.data.model.MeetingTopicEntity
import app.offlinetranscriber.mobile.data.model.QuizQuestionEntity
import app.offlinetranscriber.mobile.data.model.SegmentSpeakerAssignmentEntity
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity
import app.offlinetranscriber.mobile.data.model.SpeakerDiarizationRunEntity
import app.offlinetranscriber.mobile.data.model.SpeakerTurnEntity
import app.offlinetranscriber.mobile.data.model.StudyChapterEntity
import app.offlinetranscriber.mobile.data.model.StudyKeyPointEntity
import app.offlinetranscriber.mobile.data.model.StudyPackEntity
import app.offlinetranscriber.mobile.data.model.TranscriptCollectionCrossRef
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

class RestoreCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val workspace: RestoreWorkspace,
    private val duplicateIndexBuilder: CurrentTranscriptFingerprintIndex,
    private val settingsRestorer: PortableSettingsRestorer
) {

    suspend fun restore(
        inspection: BackupInspection,
        mode: RestoreMode,
        restoreSettings: Boolean,
        onProgress: suspend (Int) -> Unit = {},
        cancelled: () -> Boolean = { false }
    ): RestoreResult = withContext(Dispatchers.IO) {
        val payloadZip = workspace.payloadZip(inspection.sessionId)
        require(payloadZip.isFile) { "Restore payload zip is missing: ${payloadZip.absolutePath}" }

        val createdMedia = mutableListOf<File>()
        val restoredMediaDir = File(context.filesDir, "restored_media").apply { mkdirs() }
        val mediaRestorer = BackupMediaRestorer(restoredMediaDir)
        val contentClearer = UserContentClearer(database)
        val ftsRebuilder = TranscriptFtsRebuilder(database)
        val foreignKeyChecker = RoomForeignKeyChecker(database.openHelper.writableDatabase)
        val dao = database.backupDao()

        try {
            ZipFile(payloadZip).use { zip ->
                onProgress(5)

                // 1. Parse media links
                val mediaLinks = parseMediaLinks(zip)

                // 2. Build restore plan
                val currentFingerprints = if (mode == RestoreMode.MERGE) {
                    duplicateIndexBuilder.buildIndex()
                } else {
                    emptyMap()
                }

                val plan = buildRestorePlan(zip, mode, currentFingerprints, mediaLinks)
                onProgress(15)

                // 3. Stage and extract media
                val restoredMediaList = if (plan.requiredMediaTranscriptOldIds.isNotEmpty()) {
                    mediaRestorer.restoreRequired(
                        zip = zip,
                        manifest = inspection.manifest,
                        mediaLinks = mediaLinks,
                        requiredTranscriptOldIds = plan.requiredMediaTranscriptOldIds
                    ) {
                        // Throttled progress during media copy
                    }
                } else {
                    emptyList()
                }

                createdMedia.addAll(restoredMediaList.map { it.finalFile })
                val mediaByTranscriptOldId = restoredMediaList.associateBy { it.transcriptOldId }

                onProgress(30)

                val idMap = RestoreIdMap()
                // Seed duplicate transcript mappings into ID map
                idMap.transcripts.putAll(plan.duplicateTranscriptMapping)
                idMap.segments.putAll(plan.duplicateSegmentMapping)

                var importedTranscriptsCount = 0
                var importedSegmentsCount = 0L
                var restoredCollectionsCount = 0
                var restoredStudyPacksCount = 0
                var restoredAskCount = 0
                var restoredMeetingCount = 0
                var restoredSpeakerClustersCount = 0

                // 4. Atomic Database Transaction
                database.withTransaction {
                    if (mode == RestoreMode.REPLACE) {
                        contentClearer.clearForReplace()
                    }

                    if (cancelled()) throw CancellationException("Restore cancelled by user")
                    coroutineContext.ensureActive()

                    // --- Transcripts ---
                    importedTranscriptsCount = importTranscripts(
                        zip = zip,
                        dao = dao,
                        plan = plan,
                        idMap = idMap,
                        mediaByOldId = mediaByTranscriptOldId
                    )
                    onProgress(45)

                    // --- Segments ---
                    importedSegmentsCount = importSegments(
                        zip = zip,
                        dao = dao,
                        plan = plan,
                        idMap = idMap
                    )
                    onProgress(55)

                    // --- Collections & Memberships ---
                    restoredCollectionsCount = importCollectionsAndMemberships(
                        zip = zip,
                        dao = dao,
                        mode = mode,
                        idMap = idMap
                    )
                    onProgress(65)

                    // --- Bookmarks ---
                    importBookmarks(
                        zip = zip,
                        dao = dao,
                        idMap = idMap
                    )
                    onProgress(70)

                    // --- Study ---
                    restoredStudyPacksCount = importStudy(
                        zip = zip,
                        dao = dao,
                        plan = plan,
                        idMap = idMap
                    )
                    onProgress(75)

                    // --- Speaker Diarization ---
                    restoredSpeakerClustersCount = importSpeakers(
                        zip = zip,
                        dao = dao,
                        plan = plan,
                        idMap = idMap
                    )
                    onProgress(80)

                    // --- Meeting ---
                    restoredMeetingCount = importMeeting(
                        zip = zip,
                        dao = dao,
                        plan = plan,
                        idMap = idMap
                    )
                    onProgress(85)

                    // --- Ask ---
                    restoredAskCount = importAsk(
                        zip = zip,
                        dao = dao,
                        idMap = idMap
                    )
                    onProgress(90)

                    // --- Rebuild FTS & FK Check ---
                    ftsRebuilder.rebuild()
                    foreignKeyChecker.requireClean()
                }

                // 5. Restore portable settings
                if (restoreSettings) {
                    settingsRestorer.restoreWhitelisted(zip)
                }

                // 6. Replace mode cleanup of obsolete app-owned media
                if (mode == RestoreMode.REPLACE) {
                    cleanObsoleteAppOwnedMedia(createdMedia.toSet())
                }

                onProgress(100)

                RestoreResult(
                    mode = mode,
                    importedTranscripts = importedTranscriptsCount,
                    reusedDuplicateTranscripts = plan.duplicateTranscriptMapping.size,
                    importedSegments = importedSegmentsCount,
                    restoredCollections = restoredCollectionsCount,
                    restoredStudyPacks = restoredStudyPacksCount,
                    restoredAskConversations = restoredAskCount,
                    restoredMeetingPacks = restoredMeetingCount,
                    restoredSpeakerClusters = restoredSpeakerClustersCount,
                    restoredMediaFiles = createdMedia.size,
                    warnings = inspection.validationWarnings
                )
            }
        } catch (error: Throwable) {
            // Delete newly staged media on failure/rollback
            createdMedia.forEach { it.delete() }
            throw error
        } finally {
            workspace.cleanup(inspection.sessionId)
        }
    }

    private fun parseMediaLinks(zip: ZipFile): Map<Long, String> {
        val entry = zip.getEntry("data/media_links.json") ?: return emptyMap()
        val links = mutableMapOf<Long, String>()
        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var transcriptOldId: Long? = null
                var mediaId: String? = null
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "mediaId" -> mediaId = json.nextString()
                        else -> json.skipValue()
                    }
                }
                json.endObject()
                if (transcriptOldId != null && mediaId != null) {
                    links[transcriptOldId] = mediaId
                }
            }
            json.endArray()
        }
        return links
    }

    private suspend fun buildRestorePlan(
        zip: ZipFile,
        mode: RestoreMode,
        currentFingerprints: Map<String, CurrentTranscriptFingerprintIndex.ExistingTranscriptMatch>,
        mediaLinks: Map<Long, String>
    ): RestorePlan {
        if (mode == RestoreMode.REPLACE) {
            val allOldIds = mutableSetOf<Long>()
            zip.getEntry("data/transcripts.json")?.let { entry ->
                zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                    val json = JsonReader(reader)
                    json.beginArray()
                    while (json.hasNext()) {
                        json.beginObject()
                        while (json.hasNext()) {
                            if (json.nextName() == "oldId") {
                                allOldIds.add(json.nextLong())
                            } else json.skipValue()
                        }
                        json.endObject()
                    }
                    json.endArray()
                }
            }
            return RestorePlan(
                mode = mode,
                duplicateTranscriptMapping = emptyMap(),
                duplicateSegmentMapping = emptyMap(),
                newTranscriptOldIds = allOldIds,
                requiredMediaTranscriptOldIds = allOldIds.filter { it in mediaLinks }.toSet(),
                skippedStudyTranscriptOldIds = emptySet(),
                skippedMeetingTranscriptOldIds = emptySet(),
                skippedSpeakerTranscriptOldIds = emptySet()
            )
        }

        // MERGE Mode: parse backup transcripts and verify segment exact match
        val backupSegmentsByTranscript = mutableMapOf<Long, MutableList<TranscriptSegmentEntity>>()
        zip.getEntry("data/transcript_segments.json")?.let { entry ->
            zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var oldId = 0L
                    var transcriptOldId = 0L
                    var startMs = 0L
                    var endMs = 0L
                    var text = ""
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "oldId" -> oldId = json.nextLong()
                            "transcriptOldId" -> transcriptOldId = json.nextLong()
                            "startMs" -> startMs = json.nextLong()
                            "endMs" -> endMs = json.nextLong()
                            "text" -> text = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    backupSegmentsByTranscript.getOrPut(transcriptOldId) { mutableListOf() }
                        .add(TranscriptSegmentEntity(id = oldId, transcriptId = transcriptOldId, startMs = startMs, endMs = endMs, text = text))
                }
                json.endArray()
            }
        }

        val duplicateTranscriptMap = mutableMapOf<Long, Long>()
        val duplicateSegmentMap = mutableMapOf<Long, Long>()
        val newTranscriptOldIds = mutableSetOf<Long>()

        zip.getEntry("data/transcripts.json")?.let { entry ->
            zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var oldId = 0L
                    var fingerprint = ""
                    var durationMs = 0L
                    var mediaType = "AUDIO"
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "oldId" -> oldId = json.nextLong()
                            "contentFingerprint" -> fingerprint = json.nextString()
                            "audioDurationMs" -> durationMs = json.nextLong()
                            "mediaType" -> mediaType = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    val match = currentFingerprints[fingerprint]
                    val backupSegments = backupSegmentsByTranscript[oldId].orEmpty().sortedBy { it.startMs }

                    if (match != null && match.segments.size == backupSegments.size) {
                        var verified = true
                        for (i in match.segments.indices) {
                            val local = match.segments[i]
                            val backup = backupSegments[i]
                            if (local.startMs != backup.startMs || local.endMs != backup.endMs ||
                                local.text.trim() != backup.text.trim()
                            ) {
                                verified = false
                                break
                            }
                        }

                        if (verified) {
                            duplicateTranscriptMap[oldId] = match.transcriptId
                            for (i in match.segments.indices) {
                                duplicateSegmentMap[backupSegments[i].id] = match.segments[i].id
                            }
                        } else {
                            newTranscriptOldIds.add(oldId)
                        }
                    } else {
                        newTranscriptOldIds.add(oldId)
                    }
                }
                json.endArray()
            }
        }

        // For duplicate transcripts, check if local derived data already exists
        val skippedStudy = mutableSetOf<Long>()
        val skippedMeeting = mutableSetOf<Long>()
        val skippedSpeaker = mutableSetOf<Long>()

        val dao = database.backupDao()
        for ((oldId, localId) in duplicateTranscriptMap) {
            val study = dao.backupStudyPacksAfter(Long.MIN_VALUE, 500).any { it.transcriptId == localId }
            if (study) skippedStudy.add(oldId)

            val meeting = dao.backupMeetingPacksAfter(Long.MIN_VALUE, 500).any { it.transcriptId == localId }
            if (meeting) skippedMeeting.add(oldId)

            val speaker = dao.backupSpeakerClustersAfter(Long.MIN_VALUE, 500).any { it.transcriptId == localId }
            if (speaker) skippedSpeaker.add(oldId)
        }

        val requiredMedia = newTranscriptOldIds.filter { it in mediaLinks }.toSet()

        return RestorePlan(
            mode = mode,
            duplicateTranscriptMapping = duplicateTranscriptMap,
            duplicateSegmentMapping = duplicateSegmentMap,
            newTranscriptOldIds = newTranscriptOldIds,
            requiredMediaTranscriptOldIds = requiredMedia,
            skippedStudyTranscriptOldIds = skippedStudy,
            skippedMeetingTranscriptOldIds = skippedMeeting,
            skippedSpeakerTranscriptOldIds = skippedSpeaker
        )
    }

    private suspend fun importTranscripts(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        plan: RestorePlan,
        idMap: RestoreIdMap,
        mediaByOldId: Map<Long, RestoredMediaMapping>
    ): Int {
        val entry = zip.getEntry("data/transcripts.json") ?: return 0
        var count = 0

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var title = ""
                var audioFileName = ""
                var audioDurationMs = 0L
                var createdAt = System.currentTimeMillis()
                var fullText = ""
                var segmentsJson = ""
                var modelUsed = ""
                var audioUriString: String? = null
                var sourceUri = ""
                var mediaType = "AUDIO"

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "title" -> title = json.nextString()
                        "audioFileName" -> audioFileName = json.nextString()
                        "audioDurationMs" -> audioDurationMs = json.nextLong()
                        "createdAt" -> createdAt = json.nextLong()
                        "fullText" -> fullText = json.nextString()
                        "segmentsJson" -> segmentsJson = json.nextString()
                        "modelUsed" -> modelUsed = json.nextString()
                        "audioUriString" -> {
                            if (json.peek() != android.util.JsonToken.NULL) {
                                audioUriString = json.nextString()
                            } else json.nextNull()
                        }
                        "sourceUri" -> sourceUri = json.nextString()
                        "mediaType" -> mediaType = json.nextString()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                if (oldId in plan.newTranscriptOldIds) {
                    val restoredMedia = mediaByOldId[oldId]
                    val finalSourceUri = restoredMedia?.finalFile?.absolutePath ?: sourceUri
                    val finalAudioUri = restoredMedia?.finalFile?.absolutePath ?: audioUriString

                    val entity = TranscriptEntity(
                        id = 0L,
                        title = title,
                        audioFileName = audioFileName,
                        audioDurationMs = audioDurationMs,
                        createdAt = createdAt,
                        fullText = fullText,
                        segmentsJson = segmentsJson,
                        modelUsed = modelUsed,
                        audioUriString = finalAudioUri,
                        sourceUri = finalSourceUri,
                        mediaType = mediaType
                    )

                    val newId = dao.insertTranscript(entity)
                    idMap.transcripts[oldId] = newId
                    count++
                }
            }
            json.endArray()
        }
        return count
    }

    private suspend fun importSegments(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        plan: RestorePlan,
        idMap: RestoreIdMap
    ): Long {
        val entry = zip.getEntry("data/transcript_segments.json") ?: return 0L
        var count = 0L
        val batch = mutableListOf<TranscriptSegmentEntity>()
        val oldIdsBatch = mutableListOf<Long>()

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var transcriptOldId = 0L
                var startMs = 0L
                var endMs = 0L
                var text = ""

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "startMs" -> startMs = json.nextLong()
                        "endMs" -> endMs = json.nextLong()
                        "text" -> text = json.nextString()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                if (transcriptOldId in plan.newTranscriptOldIds) {
                    val newTranscriptId = idMap.requireTranscript(transcriptOldId)
                    batch.add(
                        TranscriptSegmentEntity(
                            id = 0L,
                            transcriptId = newTranscriptId,
                            startMs = startMs,
                            endMs = endMs,
                            text = text
                        )
                    )
                    oldIdsBatch.add(oldId)

                    if (batch.size >= 500) {
                        val newIds = dao.insertSegments(batch)
                        for (i in batch.indices) {
                            idMap.segments[oldIdsBatch[i]] = newIds[i]
                            count++
                        }
                        batch.clear()
                        oldIdsBatch.clear()
                    }
                }
            }
            json.endArray()
        }

        if (batch.isNotEmpty()) {
            val newIds = dao.insertSegments(batch)
            for (i in batch.indices) {
                idMap.segments[oldIdsBatch[i]] = newIds[i]
                count++
            }
            batch.clear()
            oldIdsBatch.clear()
        }

        return count
    }

    private suspend fun importCollectionsAndMemberships(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        mode: RestoreMode,
        idMap: RestoreIdMap
    ): Int {
        val entry = zip.getEntry("data/collections.json") ?: return 0
        var count = 0
        val existingNames = if (mode == RestoreMode.MERGE) {
            dao.getAllCollections().map { it.name }.toSet()
        } else {
            emptySet()
        }

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var name = ""
                var createdAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "name" -> name = json.nextString()
                        "createdAt" -> createdAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                var finalName = name
                if (mode == RestoreMode.MERGE && finalName in existingNames) {
                    finalName = "$name (Restored)"
                }

                val newId = dao.insertCollection(CollectionEntity(id = 0L, name = finalName, createdAt = createdAt))
                idMap.collections[oldId] = newId
                count++
            }
            json.endArray()
        }

        zip.getEntry("data/collection_memberships.json")?.let { memEntry ->
            zip.getInputStream(memEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var collectionOldId = 0L
                    var transcriptOldId = 0L
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "collectionOldId" -> collectionOldId = json.nextLong()
                            "transcriptOldId" -> transcriptOldId = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    val newColId = idMap.collections[collectionOldId]
                    val newTransId = idMap.transcripts[transcriptOldId]
                    if (newColId != null && newTransId != null) {
                        dao.insertCollectionMembership(TranscriptCollectionCrossRef(newTransId, newColId))
                    }
                }
                json.endArray()
            }
        }

        return count
    }

    private suspend fun importBookmarks(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        idMap: RestoreIdMap
    ) {
        val entry = zip.getEntry("data/bookmarks.json") ?: return
        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var transcriptOldId = 0L
                var segmentOldId = 0L
                var note: String? = null
                var createdAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "segmentOldId" -> segmentOldId = json.nextLong()
                        "note" -> {
                            if (json.peek() != android.util.JsonToken.NULL) {
                                note = json.nextString()
                            } else json.nextNull()
                        }
                        "createdAt" -> createdAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                val newTransId = idMap.transcripts[transcriptOldId]
                val newSegId = idMap.segments[segmentOldId]
                if (newTransId != null && newSegId != null) {
                    dao.insertBookmark(
                        BookmarkEntity(
                            id = 0L,
                            transcriptId = newTransId,
                            segmentId = newSegId,
                            note = note,
                            createdAt = createdAt
                        )
                    )
                }
            }
            json.endArray()
        }
    }

    private suspend fun importStudy(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        plan: RestorePlan,
        idMap: RestoreIdMap
    ): Int {
        val entry = zip.getEntry("data/study_packs.json") ?: return 0
        var count = 0

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var transcriptOldId = 0L
                var engine = "WHISPER_STUDY"
                var generatedAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "engine" -> engine = json.nextString()
                        "generatedAt" -> generatedAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                if (transcriptOldId !in plan.skippedStudyTranscriptOldIds) {
                    val newTransId = idMap.transcripts[transcriptOldId]
                    if (newTransId != null) {
                        val newId = dao.insertStudyPack(StudyPackEntity(id = 0L, transcriptId = newTransId, engine = engine, generatedAt = generatedAt))
                        idMap.studyPacks[oldId] = newId
                        count++
                    }
                }
            }
            json.endArray()
        }

        // Key Points
        zip.getEntry("data/study_key_points.json")?.let { keyEntry ->
            val list = mutableListOf<StudyKeyPointEntity>()
            zip.getInputStream(keyEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var spOldId = 0L
                    var position = 0
                    var text = ""
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "studyPackOldId" -> spOldId = json.nextLong()
                            "position" -> position = json.nextInt()
                            "text" -> text = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newSpId = idMap.studyPacks[spOldId]
                    if (newSpId != null) {
                        list.add(StudyKeyPointEntity(id = 0L, studyPackId = newSpId, position = position, text = text))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertStudyKeyPoints(list)
        }

        // Chapters
        zip.getEntry("data/study_chapters.json")?.let { chapEntry ->
            val list = mutableListOf<StudyChapterEntity>()
            zip.getInputStream(chapEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var spOldId = 0L
                    var position = 0
                    var title = ""
                    var startMs = 0L
                    var summary = ""
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "studyPackOldId" -> spOldId = json.nextLong()
                            "position" -> position = json.nextInt()
                            "title" -> title = json.nextString()
                            "startMs" -> startMs = json.nextLong()
                            "summary" -> summary = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newSpId = idMap.studyPacks[spOldId]
                    if (newSpId != null) {
                        list.add(StudyChapterEntity(id = 0L, studyPackId = newSpId, position = position, title = title, startMs = startMs, summary = summary))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertStudyChapters(list)
        }

        // Flashcards
        zip.getEntry("data/flashcards.json")?.let { flashEntry ->
            val list = mutableListOf<FlashcardEntity>()
            zip.getInputStream(flashEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var spOldId = 0L
                    var position = 0
                    var front = ""
                    var back = ""
                    var status = "NEW"
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "studyPackOldId" -> spOldId = json.nextLong()
                            "position" -> position = json.nextInt()
                            "front" -> front = json.nextString()
                            "back" -> back = json.nextString()
                            "status" -> status = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newSpId = idMap.studyPacks[spOldId]
                    if (newSpId != null) {
                        list.add(FlashcardEntity(id = 0L, studyPackId = newSpId, position = position, front = front, back = back, status = status))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertFlashcards(list)
        }

        // Quiz Questions
        zip.getEntry("data/quiz_questions.json")?.let { quizEntry ->
            val list = mutableListOf<QuizQuestionEntity>()
            zip.getInputStream(quizEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var spOldId = 0L
                    var position = 0
                    var question = ""
                    var optionA = ""
                    var optionB = ""
                    var optionC = ""
                    var optionD = ""
                    var correctIndex = 0
                    var explanation = ""
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "studyPackOldId" -> spOldId = json.nextLong()
                            "position" -> position = json.nextInt()
                            "question" -> question = json.nextString()
                            "optionA" -> optionA = json.nextString()
                            "optionB" -> optionB = json.nextString()
                            "optionC" -> optionC = json.nextString()
                            "optionD" -> optionD = json.nextString()
                            "correctIndex" -> correctIndex = json.nextInt()
                            "explanation" -> explanation = json.nextString()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newSpId = idMap.studyPacks[spOldId]
                    if (newSpId != null) {
                        list.add(QuizQuestionEntity(id = 0L, studyPackId = newSpId, position = position, question = question, optionA = optionA, optionB = optionB, optionC = optionC, optionD = optionD, correctIndex = correctIndex, explanation = explanation))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertQuizQuestions(list)
        }

        return count
    }

    private suspend fun importSpeakers(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        plan: RestorePlan,
        idMap: RestoreIdMap
    ): Int {
        val entry = zip.getEntry("data/speaker_clusters.json") ?: return 0
        var count = 0

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var transcriptOldId = 0L
                var speakerIndex = 0
                var customName = ""
                var userNamed = false
                var createdAt = System.currentTimeMillis()
                var updatedAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "speakerIndex" -> speakerIndex = json.nextInt()
                        "customName" -> customName = json.nextString()
                        "userNamed" -> userNamed = json.nextBoolean()
                        "createdAt" -> createdAt = json.nextLong()
                        "updatedAt" -> updatedAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                if (transcriptOldId !in plan.skippedSpeakerTranscriptOldIds) {
                    val newTransId = idMap.transcripts[transcriptOldId]
                    if (newTransId != null) {
                        val newId = dao.insertSpeakerCluster(
                            SpeakerClusterEntity(
                                id = 0L,
                                transcriptId = newTransId,
                                speakerIndex = speakerIndex,
                                customName = customName,
                                userNamed = userNamed,
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                        idMap.speakerClusters[oldId] = newId
                        count++
                    }
                }
            }
            json.endArray()
        }

        // Speaker Turns
        zip.getEntry("data/speaker_turns.json")?.let { turnsEntry ->
            val list = mutableListOf<SpeakerTurnEntity>()
            zip.getInputStream(turnsEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var transOldId = 0L
                    var clusterOldId = 0L
                    var startMs = 0L
                    var endMs = 0L
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "transcriptOldId" -> transOldId = json.nextLong()
                            "speakerClusterOldId" -> clusterOldId = json.nextLong()
                            "startMs" -> startMs = json.nextLong()
                            "endMs" -> endMs = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newTransId = idMap.transcripts[transOldId]
                    val newClusterId = idMap.speakerClusters[clusterOldId]
                    if (newTransId != null && newClusterId != null) {
                        list.add(SpeakerTurnEntity(id = 0L, transcriptId = newTransId, speakerClusterId = newClusterId, startMs = startMs, endMs = endMs))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertSpeakerTurns(list)
        }

        // Speaker Assignments
        zip.getEntry("data/segment_speaker_assignments.json")?.let { assignEntry ->
            val list = mutableListOf<SegmentSpeakerAssignmentEntity>()
            zip.getInputStream(assignEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var segOldId = 0L
                    var clusterOldId = 0L
                    var overlapRatio = 0f
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "segmentOldId" -> segOldId = json.nextLong()
                            "speakerClusterOldId" -> clusterOldId = json.nextLong()
                            "overlapRatio" -> overlapRatio = json.nextDouble().toFloat()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newSegId = idMap.segments[segOldId]
                    val newClusterId = idMap.speakerClusters[clusterOldId]
                    if (newSegId != null && newClusterId != null) {
                        list.add(SegmentSpeakerAssignmentEntity(segmentId = newSegId, speakerClusterId = newClusterId, overlapRatio = overlapRatio))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertSegmentSpeakerAssignments(list)
        }

        // Diarization Runs
        zip.getEntry("data/speaker_diarization_runs.json")?.let { runsEntry ->
            zip.getInputStream(runsEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var transOldId = 0L
                    var status = "COMPLETED"
                    var progress = 100
                    var countMode = "AUTO"
                    var reqCount: Int? = null
                    var detCount = 0
                    var engVer = "1.0"
                    var segModel = ""
                    var embModel = ""
                    var errMsg: String? = null
                    var cAt = System.currentTimeMillis()
                    var uAt = System.currentTimeMillis()

                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "transcriptOldId" -> transOldId = json.nextLong()
                            "status" -> status = json.nextString()
                            "progress" -> progress = json.nextInt()
                            "speakerCountMode" -> countMode = json.nextString()
                            "requestedSpeakerCount" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    reqCount = json.nextInt()
                                } else json.nextNull()
                            }
                            "detectedSpeakerCount" -> detCount = json.nextInt()
                            "engineVersion" -> engVer = json.nextString()
                            "segmentationModelId" -> segModel = json.nextString()
                            "embeddingModelId" -> embModel = json.nextString()
                            "errorMessage" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    errMsg = json.nextString()
                                } else json.nextNull()
                            }
                            "createdAt" -> cAt = json.nextLong()
                            "updatedAt" -> uAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    if (transOldId !in plan.skippedSpeakerTranscriptOldIds) {
                        val newTransId = idMap.transcripts[transOldId]
                        if (newTransId != null) {
                            dao.insertSpeakerDiarizationRun(
                                SpeakerDiarizationRunEntity(
                                    id = 0L,
                                    transcriptId = newTransId,
                                    status = status,
                                    progress = progress,
                                    speakerCountMode = countMode,
                                    requestedSpeakerCount = reqCount,
                                    detectedSpeakerCount = detCount,
                                    engineVersion = engVer,
                                    segmentationModelId = segModel,
                                    embeddingModelId = embModel,
                                    errorMessage = errMsg,
                                    createdAt = cAt,
                                    updatedAt = uAt
                                )
                            )
                        }
                    }
                }
                json.endArray()
            }
        }

        return count
    }

    private suspend fun importMeeting(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        plan: RestorePlan,
        idMap: RestoreIdMap
    ): Int {
        val entry = zip.getEntry("data/meeting_packs.json") ?: return 0
        var count = 0

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var transcriptOldId = 0L
                var summary = ""
                var engine = "WHISPER_MEETING"
                var createdAt = System.currentTimeMillis()
                var updatedAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "transcriptOldId" -> transcriptOldId = json.nextLong()
                        "summary" -> summary = json.nextString()
                        "engine" -> engine = json.nextString()
                        "createdAt" -> createdAt = json.nextLong()
                        "updatedAt" -> updatedAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                if (transcriptOldId !in plan.skippedMeetingTranscriptOldIds) {
                    val newTransId = idMap.transcripts[transcriptOldId]
                    if (newTransId != null) {
                        val newId = dao.insertMeetingPack(
                            MeetingPackEntity(
                                id = 0L,
                                transcriptId = newTransId,
                                summary = summary,
                                engine = engine,
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                        )
                        idMap.meetingPacks[oldId] = newId
                        count++
                    }
                }
            }
            json.endArray()
        }

        // Summary Citations
        zip.getEntry("data/meeting_summary_citations.json")?.let { citEntry ->
            val list = mutableListOf<MeetingSummaryCitationEntity>()
            zip.getInputStream(citEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var mpOldId = 0L
                    var segOldId = 0L
                    var pos = 0
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "meetingPackOldId" -> mpOldId = json.nextLong()
                            "segmentOldId" -> segOldId = json.nextLong()
                            "position" -> pos = json.nextInt()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newMpId = idMap.meetingPacks[mpOldId]
                    val newSegId = idMap.segments[segOldId]
                    if (newMpId != null && newSegId != null) {
                        list.add(MeetingSummaryCitationEntity(meetingPackId = newMpId, segmentId = newSegId, position = pos))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertMeetingSummaryCitations(list)
        }

        // Actions
        zip.getEntry("data/meeting_actions.json")?.let { actEntry ->
            val list = mutableListOf<MeetingActionEntity>()
            zip.getInputStream(actEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var mpOldId = 0L
                    var text = ""
                    var assignee = ""
                    var dueText = ""
                    var segOldId: Long? = null
                    var startMs = 0L
                    var status = "OPEN"
                    var edited = false
                    var cAt = System.currentTimeMillis()
                    var uAt = System.currentTimeMillis()

                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "meetingPackOldId" -> mpOldId = json.nextLong()
                            "text" -> text = json.nextString()
                            "assignee" -> assignee = json.nextString()
                            "dueText" -> dueText = json.nextString()
                            "sourceSegmentOldId" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    segOldId = json.nextLong()
                                } else json.nextNull()
                            }
                            "startMs" -> startMs = json.nextLong()
                            "status" -> status = json.nextString()
                            "manuallyEdited" -> edited = json.nextBoolean()
                            "createdAt" -> cAt = json.nextLong()
                            "updatedAt" -> uAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    val newMpId = idMap.meetingPacks[mpOldId]
                    if (newMpId != null) {
                        val newSegId = segOldId?.let { idMap.segments[it] }
                        list.add(
                            MeetingActionEntity(
                                id = 0L,
                                meetingPackId = newMpId,
                                text = text,
                                assignee = assignee,
                                dueText = dueText,
                                sourceSegmentId = newSegId,
                                startMs = startMs,
                                status = status,
                                manuallyEdited = edited,
                                createdAt = cAt,
                                updatedAt = uAt
                            )
                        )
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertMeetingActions(list)
        }

        // Decisions
        zip.getEntry("data/meeting_decisions.json")?.let { decEntry ->
            val list = mutableListOf<MeetingDecisionEntity>()
            zip.getInputStream(decEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var mpOldId = 0L
                    var text = ""
                    var segOldId: Long? = null
                    var startMs = 0L
                    var cAt = System.currentTimeMillis()
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "meetingPackOldId" -> mpOldId = json.nextLong()
                            "text" -> text = json.nextString()
                            "sourceSegmentOldId" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    segOldId = json.nextLong()
                                } else json.nextNull()
                            }
                            "startMs" -> startMs = json.nextLong()
                            "createdAt" -> cAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newMpId = idMap.meetingPacks[mpOldId]
                    if (newMpId != null) {
                        val newSegId = segOldId?.let { idMap.segments[it] }
                        list.add(MeetingDecisionEntity(id = 0L, meetingPackId = newMpId, text = text, sourceSegmentId = newSegId, startMs = startMs, createdAt = cAt))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertMeetingDecisions(list)
        }

        // Questions
        zip.getEntry("data/meeting_questions.json")?.let { qEntry ->
            val list = mutableListOf<MeetingQuestionEntity>()
            zip.getInputStream(qEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var mpOldId = 0L
                    var text = ""
                    var segOldId: Long? = null
                    var startMs = 0L
                    var cAt = System.currentTimeMillis()
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "meetingPackOldId" -> mpOldId = json.nextLong()
                            "text" -> text = json.nextString()
                            "sourceSegmentOldId" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    segOldId = json.nextLong()
                                } else json.nextNull()
                            }
                            "startMs" -> startMs = json.nextLong()
                            "createdAt" -> cAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newMpId = idMap.meetingPacks[mpOldId]
                    if (newMpId != null) {
                        val newSegId = segOldId?.let { idMap.segments[it] }
                        list.add(MeetingQuestionEntity(id = 0L, meetingPackId = newMpId, text = text, sourceSegmentId = newSegId, startMs = startMs, createdAt = cAt))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertMeetingQuestions(list)
        }

        // Topics
        zip.getEntry("data/meeting_topics.json")?.let { topEntry ->
            val list = mutableListOf<MeetingTopicEntity>()
            zip.getInputStream(topEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var mpOldId = 0L
                    var title = ""
                    var segOldId: Long? = null
                    var startMs = 0L
                    var cAt = System.currentTimeMillis()
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "meetingPackOldId" -> mpOldId = json.nextLong()
                            "title" -> title = json.nextString()
                            "sourceSegmentOldId" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    segOldId = json.nextLong()
                                } else json.nextNull()
                            }
                            "startMs" -> startMs = json.nextLong()
                            "createdAt" -> cAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()
                    val newMpId = idMap.meetingPacks[mpOldId]
                    if (newMpId != null) {
                        val newSegId = segOldId?.let { idMap.segments[it] }
                        list.add(MeetingTopicEntity(id = 0L, meetingPackId = newMpId, title = title, sourceSegmentId = newSegId, startMs = startMs, createdAt = cAt))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertMeetingTopics(list)
        }

        return count
    }

    private suspend fun importAsk(
        zip: ZipFile,
        dao: app.offlinetranscriber.mobile.data.database.BackupDao,
        idMap: RestoreIdMap
    ): Int {
        val entry = zip.getEntry("data/ask_conversations.json") ?: return 0
        var count = 0

        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
            val json = JsonReader(reader)
            json.beginArray()
            while (json.hasNext()) {
                json.beginObject()
                var oldId = 0L
                var scope = "LIBRARY"
                var transcriptOldId: Long? = null
                var title = ""
                var createdAt = System.currentTimeMillis()
                var updatedAt = System.currentTimeMillis()

                while (json.hasNext()) {
                    when (json.nextName()) {
                        "oldId" -> oldId = json.nextLong()
                        "scope" -> scope = json.nextString()
                        "transcriptOldId" -> {
                            if (json.peek() != android.util.JsonToken.NULL) {
                                transcriptOldId = json.nextLong()
                            } else json.nextNull()
                        }
                        "title" -> title = json.nextString()
                        "createdAt" -> createdAt = json.nextLong()
                        "updatedAt" -> updatedAt = json.nextLong()
                        else -> json.skipValue()
                    }
                }
                json.endObject()

                val newTransId = transcriptOldId?.let { idMap.transcripts[it] }
                val newId = dao.insertAskConversation(
                    AskConversationEntity(
                        id = 0L,
                        scope = scope,
                        transcriptId = newTransId,
                        title = title,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
                idMap.askConversations[oldId] = newId
                count++
            }
            json.endArray()
        }

        // Ask Messages
        zip.getEntry("data/ask_messages.json")?.let { msgEntry ->
            zip.getInputStream(msgEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var oldId = 0L
                    var convOldId = 0L
                    var role = "user"
                    var text = ""
                    var engine: String? = null
                    var insufficient = false
                    var createdAt = System.currentTimeMillis()

                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "oldId" -> oldId = json.nextLong()
                            "conversationOldId" -> convOldId = json.nextLong()
                            "role" -> role = json.nextString()
                            "text" -> text = json.nextString()
                            "engine" -> {
                                if (json.peek() != android.util.JsonToken.NULL) {
                                    engine = json.nextString()
                                } else json.nextNull()
                            }
                            "insufficientEvidence" -> insufficient = json.nextBoolean()
                            "createdAt" -> createdAt = json.nextLong()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    val newConvId = idMap.askConversations[convOldId]
                    if (newConvId != null) {
                        val newId = dao.insertAskMessage(
                            AskMessageEntity(
                                id = 0L,
                                conversationId = newConvId,
                                role = role,
                                text = text,
                                engine = engine,
                                insufficientEvidence = insufficient,
                                createdAt = createdAt
                            )
                        )
                        idMap.askMessages[oldId] = newId
                    }
                }
                json.endArray()
            }
        }

        // Ask Citations
        zip.getEntry("data/ask_citations.json")?.let { citEntry ->
            val list = mutableListOf<AskCitationEntity>()
            zip.getInputStream(citEntry).bufferedReader(Charsets.UTF_8).use { reader ->
                val json = JsonReader(reader)
                json.beginArray()
                while (json.hasNext()) {
                    json.beginObject()
                    var msgOldId = 0L
                    var segOldId = 0L
                    var pos = 0
                    while (json.hasNext()) {
                        when (json.nextName()) {
                            "messageOldId" -> msgOldId = json.nextLong()
                            "segmentOldId" -> segOldId = json.nextLong()
                            "position" -> pos = json.nextInt()
                            else -> json.skipValue()
                        }
                    }
                    json.endObject()

                    val newMsgId = idMap.askMessages[msgOldId]
                    val newSegId = idMap.segments[segOldId]
                    if (newMsgId != null && newSegId != null) {
                        list.add(AskCitationEntity(id = 0L, messageId = newMsgId, segmentId = newSegId, position = pos))
                    }
                }
                json.endArray()
            }
            if (list.isNotEmpty()) dao.insertAskCitations(list)
        }

        return count
    }

    private fun cleanObsoleteAppOwnedMedia(keepFiles: Set<File>) {
        val dir = File(context.filesDir, "restored_media")
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles().orEmpty()
                .filter { it.isFile && it !in keepFiles }
                .forEach { it.delete() }
        }
    }
}
