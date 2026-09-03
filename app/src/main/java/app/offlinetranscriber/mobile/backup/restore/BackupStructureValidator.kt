package app.offlinetranscriber.mobile.backup.restore

import app.offlinetranscriber.mobile.backup.format.BackupManifest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.zip.ZipFile

class BackupStructureValidator {

    private val json = Json { ignoreUnknownKeys = true }

    fun validate(zip: ZipFile, manifest: BackupManifest) {
        val transcriptIds = HashSet<Long>()
        val segmentIds = HashSet<Long>()
        val collectionIds = HashSet<Long>()
        val studyPackIds = HashSet<Long>()
        val askConversationIds = HashSet<Long>()
        val askMessageIds = HashSet<Long>()
        val meetingPackIds = HashSet<Long>()
        val speakerClusterIds = HashSet<Long>()

        val manifestMediaIds = manifest.media.map { it.mediaId }.toSet()

        fun readJsonArray(entryName: String): JsonArray? {
            val entry = zip.getEntry(entryName) ?: return null
            val text = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            return json.parseToJsonElement(text).jsonArray
        }

        // 1. TRANSCRIPTS
        readJsonArray("data/transcripts.json")?.let { array ->
            require(array.size <= BackupSafetyLimits.MAX_SECTION_ROWS) { "Transcript count exceeds safety limit" }
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val durationMs = obj["audioDurationMs"]?.jsonPrimitive?.longOrNull ?: 0L

                val id = checkNotNull(oldId) { "Missing oldId in transcript" }
                require(transcriptIds.add(id)) { "Duplicate transcript oldId: $id" }
                require(durationMs >= 0L) { "Negative audio duration in transcript: $id" }
            }
        }

        // 2. SEGMENTS
        readJsonArray("data/transcript_segments.json")?.let { array ->
            require(array.size <= BackupSafetyLimits.MAX_SECTION_ROWS) { "Segment count exceeds safety limit" }
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull
                val startMs = obj["startMs"]?.jsonPrimitive?.longOrNull ?: 0L
                val endMs = obj["endMs"]?.jsonPrimitive?.longOrNull ?: 0L

                val id = checkNotNull(oldId) { "Missing oldId in segment" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in segment" }
                require(tId in transcriptIds) { "Segment $id references missing transcript $tId" }
                require(segmentIds.add(id)) { "Duplicate segment oldId: $id" }
                require(startMs >= 0L && endMs >= startMs) { "Invalid segment timing: $startMs..$endMs in segment $id" }
            }
        }

        // 3. BOOKMARKS
        readJsonArray("data/bookmarks.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull
                val segmentOldId = obj["segmentOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in bookmark" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in bookmark $id" }
                val sId = checkNotNull(segmentOldId) { "Missing segmentOldId in bookmark $id" }
                require(tId in transcriptIds) { "Bookmark $id references missing transcript $tId" }
                require(sId in segmentIds) { "Bookmark $id references missing segment $sId" }
            }
        }

        // 4. COLLECTIONS & MEMBERSHIPS
        readJsonArray("data/collections.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val id = checkNotNull(oldId) { "Missing oldId in collection" }
                require(collectionIds.add(id)) { "Duplicate collection oldId: $id" }
            }
        }

        readJsonArray("data/collection_memberships.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val collectionOldId = obj["collectionOldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull

                val cId = checkNotNull(collectionOldId) { "Missing collectionOldId in membership" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in membership" }
                require(cId in collectionIds) { "Membership references missing collection $cId" }
                require(tId in transcriptIds) { "Membership references missing transcript $tId" }
            }
        }

        // 5. STUDY PACKS & CHILDREN
        readJsonArray("data/study_packs.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in study pack" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in study pack $id" }
                require(tId in transcriptIds) { "Study pack $id references missing transcript $tId" }
                require(studyPackIds.add(id)) { "Duplicate study pack oldId: $id" }
            }
        }

        fun validateStudyChildren(entryName: String, name: String) {
            readJsonArray(entryName)?.let { array ->
                for (elem in array) {
                    val obj = elem.jsonObject
                    val studyPackOldId = obj["studyPackOldId"]?.jsonPrimitive?.longOrNull
                    val spId = checkNotNull(studyPackOldId) { "Missing studyPackOldId in $name" }
                    require(spId in studyPackIds) { "$name references missing study pack $spId" }
                }
            }
        }

        validateStudyChildren("data/study_key_points.json", "Study key point")
        validateStudyChildren("data/study_chapters.json", "Study chapter")
        validateStudyChildren("data/flashcards.json", "Flashcard")
        validateStudyChildren("data/quiz_questions.json", "Quiz question")

        // 6. ASK CONVERSATIONS, MESSAGES & CITATIONS
        readJsonArray("data/ask_conversations.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in ask conversation" }
                if (transcriptOldId != null) {
                    require(transcriptOldId in transcriptIds) { "Ask conversation $id references missing transcript $transcriptOldId" }
                }
                require(askConversationIds.add(id)) { "Duplicate ask conversation oldId: $id" }
            }
        }

        readJsonArray("data/ask_messages.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val conversationOldId = obj["conversationOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in ask message" }
                val cId = checkNotNull(conversationOldId) { "Missing conversationOldId in ask message $id" }
                require(cId in askConversationIds) { "Ask message $id references missing conversation $cId" }
                require(askMessageIds.add(id)) { "Duplicate ask message oldId: $id" }
            }
        }

        readJsonArray("data/ask_citations.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val messageOldId = obj["messageOldId"]?.jsonPrimitive?.longOrNull
                val segmentOldId = obj["segmentOldId"]?.jsonPrimitive?.longOrNull

                val mId = checkNotNull(messageOldId) { "Missing messageOldId in ask citation" }
                val sId = checkNotNull(segmentOldId) { "Missing segmentOldId in ask citation" }
                require(mId in askMessageIds) { "Ask citation references missing message $mId" }
                require(sId in segmentIds) { "Ask citation references missing segment $sId" }
            }
        }

        // 7. MEETING PACKS & CHILDREN
        readJsonArray("data/meeting_packs.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in meeting pack" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in meeting pack $id" }
                require(tId in transcriptIds) { "Meeting pack $id references missing transcript $tId" }
                require(meetingPackIds.add(id)) { "Duplicate meeting pack oldId: $id" }
            }
        }

        readJsonArray("data/meeting_summary_citations.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val meetingPackOldId = obj["meetingPackOldId"]?.jsonPrimitive?.longOrNull
                val segmentOldId = obj["segmentOldId"]?.jsonPrimitive?.longOrNull

                val mpId = checkNotNull(meetingPackOldId) { "Missing meetingPackOldId in citation" }
                val sId = checkNotNull(segmentOldId) { "Missing segmentOldId in citation" }
                require(mpId in meetingPackIds) { "Citation references missing meeting pack $mpId" }
                require(sId in segmentIds) { "Citation references missing segment $sId" }
            }
        }

        fun validateMeetingChildren(entryName: String, name: String) {
            readJsonArray(entryName)?.let { array ->
                for (elem in array) {
                    val obj = elem.jsonObject
                    val meetingPackOldId = obj["meetingPackOldId"]?.jsonPrimitive?.longOrNull
                    val sourceSegmentOldId = obj["sourceSegmentOldId"]?.jsonPrimitive?.longOrNull

                    val mpId = checkNotNull(meetingPackOldId) { "Missing meetingPackOldId in $name" }
                    require(mpId in meetingPackIds) { "$name references missing meeting pack $mpId" }
                    if (sourceSegmentOldId != null) {
                        require(sourceSegmentOldId in segmentIds) { "$name references missing segment $sourceSegmentOldId" }
                    }
                }
            }
        }

        validateMeetingChildren("data/meeting_actions.json", "Meeting action")
        validateMeetingChildren("data/meeting_decisions.json", "Meeting decision")
        validateMeetingChildren("data/meeting_questions.json", "Meeting question")
        validateMeetingChildren("data/meeting_topics.json", "Meeting topic")

        // 8. SPEAKER CLUSTERS, TURNS & ASSIGNMENTS
        readJsonArray("data/speaker_clusters.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val oldId = obj["oldId"]?.jsonPrimitive?.longOrNull
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull

                val id = checkNotNull(oldId) { "Missing oldId in speaker cluster" }
                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in speaker cluster $id" }
                require(tId in transcriptIds) { "Speaker cluster $id references missing transcript $tId" }
                require(speakerClusterIds.add(id)) { "Duplicate speaker cluster oldId: $id" }
            }
        }

        readJsonArray("data/speaker_turns.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull
                val speakerClusterOldId = obj["speakerClusterOldId"]?.jsonPrimitive?.longOrNull
                val startMs = obj["startMs"]?.jsonPrimitive?.longOrNull ?: 0L
                val endMs = obj["endMs"]?.jsonPrimitive?.longOrNull ?: 0L

                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in turn" }
                val scId = checkNotNull(speakerClusterOldId) { "Missing speakerClusterOldId in turn" }
                require(tId in transcriptIds) { "Turn references missing transcript $tId" }
                require(scId in speakerClusterIds) { "Turn references missing speaker cluster $scId" }
                require(startMs >= 0L && endMs >= startMs) { "Invalid turn timing $startMs..$endMs" }
            }
        }

        readJsonArray("data/segment_speaker_assignments.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val segmentOldId = obj["segmentOldId"]?.jsonPrimitive?.longOrNull
                val speakerClusterOldId = obj["speakerClusterOldId"]?.jsonPrimitive?.longOrNull
                val overlapRatio = obj["overlapRatio"]?.jsonPrimitive?.doubleOrNull ?: 0.0

                val sId = checkNotNull(segmentOldId) { "Missing segmentOldId in assignment" }
                val scId = checkNotNull(speakerClusterOldId) { "Missing speakerClusterOldId in assignment" }
                require(sId in segmentIds) { "Assignment references missing segment $sId" }
                require(scId in speakerClusterIds) { "Assignment references missing speaker cluster $scId" }
                require(!overlapRatio.isNaN() && !overlapRatio.isInfinite() && overlapRatio in 0.0..1.0) {
                    "Invalid assignment overlap ratio: $overlapRatio"
                }
            }
        }

        // 9. MEDIA LINKS
        readJsonArray("data/media_links.json")?.let { array ->
            for (elem in array) {
                val obj = elem.jsonObject
                val transcriptOldId = obj["transcriptOldId"]?.jsonPrimitive?.longOrNull
                val mediaId = obj["mediaId"]?.jsonPrimitive?.content

                val tId = checkNotNull(transcriptOldId) { "Missing transcriptOldId in media link" }
                val mId = checkNotNull(mediaId) { "Missing mediaId in media link" }
                require(tId in transcriptIds) { "Media link references missing transcript $tId" }
                require(mId in manifestMediaIds) { "Media link references missing media entry $mId" }
            }
        }
    }
}
