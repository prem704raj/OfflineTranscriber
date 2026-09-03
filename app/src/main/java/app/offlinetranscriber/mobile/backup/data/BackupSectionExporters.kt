package app.offlinetranscriber.mobile.backup.data

import android.util.JsonWriter
import app.offlinetranscriber.mobile.backup.fingerprint.FingerprintSegment
import app.offlinetranscriber.mobile.backup.fingerprint.TranscriptFingerprint
import app.offlinetranscriber.mobile.data.database.BackupDao
import app.offlinetranscriber.mobile.settings.AppSettingsRepository
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.io.OutputStreamWriter

class TranscriptBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/transcripts.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

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

                writer.beginObject()
                writer.name("oldId").value(t.id)
                writer.name("title").value(t.title)
                writer.name("audioFileName").value(t.audioFileName)
                writer.name("audioDurationMs").value(t.audioDurationMs)
                writer.name("createdAt").value(t.createdAt)
                writer.name("fullText").value(t.fullText)
                writer.name("segmentsJson").value(t.segmentsJson)
                writer.name("modelUsed").value(t.modelUsed)
                if (t.audioUriString != null) {
                    writer.name("audioUriString").value(t.audioUriString)
                } else {
                    writer.name("audioUriString").nullValue()
                }
                writer.name("sourceUri").value(t.sourceUri)
                writer.name("mediaType").value(t.mediaType)
                writer.name("contentFingerprint").value(fingerprint)
                writer.endObject()

                afterId = t.id
                count++
                if (count % 100L == 0L) {
                    writer.flush()
                    onRows(count)
                }
            }
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class TranscriptSegmentsBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/transcript_segments.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupSegmentsAfter(afterId, 750)
            if (page.isEmpty()) break

            for (s in page) {
                writer.beginObject()
                writer.name("oldId").value(s.id)
                writer.name("transcriptOldId").value(s.transcriptId)
                writer.name("startMs").value(s.startMs)
                writer.name("endMs").value(s.endMs)
                writer.name("text").value(s.text)
                writer.endObject()

                afterId = s.id
                count++
                if (count % 250L == 0L) {
                    writer.flush()
                    onRows(count)
                }
            }
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class BookmarkBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/bookmarks.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupBookmarksAfter(afterId, 500)
            if (page.isEmpty()) break

            for (b in page) {
                writer.beginObject()
                writer.name("oldId").value(b.id)
                writer.name("transcriptOldId").value(b.transcriptId)
                writer.name("segmentOldId").value(b.segmentId)
                if (b.note != null) writer.name("note").value(b.note) else writer.name("note").nullValue()
                writer.name("createdAt").value(b.createdAt)
                writer.endObject()

                afterId = b.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class CollectionBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/collections.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupCollectionsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("oldId").value(c.id)
                writer.name("name").value(c.name)
                writer.name("createdAt").value(c.createdAt)
                writer.endObject()

                afterId = c.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class CollectionMembershipBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/collection_memberships.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var offset = 0
        var count = 0L

        while (true) {
            val page = dao.backupCollectionMemberships(500, offset)
            if (page.isEmpty()) break

            for (m in page) {
                writer.beginObject()
                writer.name("collectionOldId").value(m.collectionId)
                writer.name("transcriptOldId").value(m.transcriptId)
                writer.endObject()
                count++
            }

            offset += page.size
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class StudyPackBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/study_packs.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupStudyPacksAfter(afterId, 500)
            if (page.isEmpty()) break

            for (p in page) {
                writer.beginObject()
                writer.name("oldId").value(p.id)
                writer.name("transcriptOldId").value(p.transcriptId)
                writer.name("engine").value(p.engine)
                writer.name("generatedAt").value(p.generatedAt)
                writer.endObject()

                afterId = p.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class StudyKeyPointBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/study_key_points.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupStudyKeyPointsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (k in page) {
                writer.beginObject()
                writer.name("oldId").value(k.id)
                writer.name("studyPackOldId").value(k.studyPackId)
                writer.name("position").value(k.position.toLong())
                writer.name("text").value(k.text)
                writer.endObject()

                afterId = k.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class StudyChapterBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/study_chapters.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupStudyChaptersAfter(afterId, 500)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("oldId").value(c.id)
                writer.name("studyPackOldId").value(c.studyPackId)
                writer.name("position").value(c.position.toLong())
                writer.name("title").value(c.title)
                writer.name("startMs").value(c.startMs)
                writer.name("summary").value(c.summary)
                writer.endObject()

                afterId = c.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class FlashcardBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/flashcards.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupFlashcardsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (f in page) {
                writer.beginObject()
                writer.name("oldId").value(f.id)
                writer.name("studyPackOldId").value(f.studyPackId)
                writer.name("position").value(f.position.toLong())
                writer.name("front").value(f.front)
                writer.name("back").value(f.back)
                writer.name("status").value(f.status)
                writer.endObject()

                afterId = f.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class QuizQuestionBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/quiz_questions.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupQuizQuestionsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (q in page) {
                writer.beginObject()
                writer.name("oldId").value(q.id)
                writer.name("studyPackOldId").value(q.studyPackId)
                writer.name("position").value(q.position.toLong())
                writer.name("question").value(q.question)
                writer.name("optionA").value(q.optionA)
                writer.name("optionB").value(q.optionB)
                writer.name("optionC").value(q.optionC)
                writer.name("optionD").value(q.optionD)
                writer.name("correctIndex").value(q.correctIndex.toLong())
                writer.name("explanation").value(q.explanation)
                writer.endObject()

                afterId = q.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class AskConversationBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/ask_conversations.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupAskConversationsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("oldId").value(c.id)
                writer.name("scope").value(c.scope)
                if (c.transcriptId != null) {
                    writer.name("transcriptOldId").value(c.transcriptId)
                } else {
                    writer.name("transcriptOldId").nullValue()
                }
                writer.name("title").value(c.title)
                writer.name("createdAt").value(c.createdAt)
                writer.name("updatedAt").value(c.updatedAt)
                writer.endObject()

                afterId = c.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class AskMessageBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/ask_messages.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupAskMessagesAfter(afterId, 500)
            if (page.isEmpty()) break

            for (m in page) {
                writer.beginObject()
                writer.name("oldId").value(m.id)
                writer.name("conversationOldId").value(m.conversationId)
                writer.name("role").value(m.role)
                writer.name("text").value(m.text)
                if (m.engine != null) writer.name("engine").value(m.engine) else writer.name("engine").nullValue()
                writer.name("insufficientEvidence").value(m.insufficientEvidence)
                writer.name("createdAt").value(m.createdAt)
                writer.endObject()

                afterId = m.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class AskCitationBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/ask_citations.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupAskCitationsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("oldId").value(c.id)
                writer.name("messageOldId").value(c.messageId)
                writer.name("segmentOldId").value(c.segmentId)
                writer.name("position").value(c.position.toLong())
                writer.endObject()

                afterId = c.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingPackBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_packs.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupMeetingPacksAfter(afterId, 500)
            if (page.isEmpty()) break

            for (p in page) {
                writer.beginObject()
                writer.name("oldId").value(p.id)
                writer.name("transcriptOldId").value(p.transcriptId)
                writer.name("summary").value(p.summary)
                writer.name("engine").value(p.engine)
                writer.name("createdAt").value(p.createdAt)
                writer.name("updatedAt").value(p.updatedAt)
                writer.endObject()

                afterId = p.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingSummaryCitationBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_summary_citations.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var offset = 0
        var count = 0L

        while (true) {
            val page = dao.backupMeetingSummaryCitations(500, offset)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("meetingPackOldId").value(c.meetingPackId)
                writer.name("segmentOldId").value(c.segmentId)
                writer.name("position").value(c.position.toLong())
                writer.endObject()
                count++
            }

            offset += page.size
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingActionBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_actions.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupMeetingActionsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (a in page) {
                writer.beginObject()
                writer.name("oldId").value(a.id)
                writer.name("meetingPackOldId").value(a.meetingPackId)
                writer.name("text").value(a.text)
                writer.name("assignee").value(a.assignee)
                writer.name("dueText").value(a.dueText)
                if (a.sourceSegmentId != null) {
                    writer.name("sourceSegmentOldId").value(a.sourceSegmentId)
                } else {
                    writer.name("sourceSegmentOldId").nullValue()
                }
                writer.name("startMs").value(a.startMs)
                writer.name("status").value(a.status)
                writer.name("manuallyEdited").value(a.manuallyEdited)
                writer.name("createdAt").value(a.createdAt)
                writer.name("updatedAt").value(a.updatedAt)
                writer.endObject()

                afterId = a.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingDecisionBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_decisions.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupMeetingDecisionsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (d in page) {
                writer.beginObject()
                writer.name("oldId").value(d.id)
                writer.name("meetingPackOldId").value(d.meetingPackId)
                writer.name("text").value(d.text)
                if (d.sourceSegmentId != null) {
                    writer.name("sourceSegmentOldId").value(d.sourceSegmentId)
                } else {
                    writer.name("sourceSegmentOldId").nullValue()
                }
                writer.name("startMs").value(d.startMs)
                writer.name("createdAt").value(d.createdAt)
                writer.endObject()

                afterId = d.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingQuestionBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_questions.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupMeetingQuestionsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (q in page) {
                writer.beginObject()
                writer.name("oldId").value(q.id)
                writer.name("meetingPackOldId").value(q.meetingPackId)
                writer.name("text").value(q.text)
                if (q.sourceSegmentId != null) {
                    writer.name("sourceSegmentOldId").value(q.sourceSegmentId)
                } else {
                    writer.name("sourceSegmentOldId").nullValue()
                }
                writer.name("startMs").value(q.startMs)
                writer.name("createdAt").value(q.createdAt)
                writer.endObject()

                afterId = q.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class MeetingTopicBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/meeting_topics.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupMeetingTopicsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (t in page) {
                writer.beginObject()
                writer.name("oldId").value(t.id)
                writer.name("meetingPackOldId").value(t.meetingPackId)
                writer.name("title").value(t.title)
                if (t.sourceSegmentId != null) {
                    writer.name("sourceSegmentOldId").value(t.sourceSegmentId)
                } else {
                    writer.name("sourceSegmentOldId").nullValue()
                }
                writer.name("startMs").value(t.startMs)
                writer.name("createdAt").value(t.createdAt)
                writer.endObject()

                afterId = t.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class SpeakerClusterBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/speaker_clusters.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupSpeakerClustersAfter(afterId, 500)
            if (page.isEmpty()) break

            for (c in page) {
                writer.beginObject()
                writer.name("oldId").value(c.id)
                writer.name("transcriptOldId").value(c.transcriptId)
                writer.name("speakerIndex").value(c.speakerIndex.toLong())
                writer.name("customName").value(c.customName)
                writer.name("userNamed").value(c.userNamed)
                writer.name("createdAt").value(c.createdAt)
                writer.name("updatedAt").value(c.updatedAt)
                writer.endObject()

                afterId = c.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class SpeakerTurnBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/speaker_turns.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupSpeakerTurnsAfter(afterId, 750)
            if (page.isEmpty()) break

            for (t in page) {
                writer.beginObject()
                writer.name("oldId").value(t.id)
                writer.name("transcriptOldId").value(t.transcriptId)
                writer.name("speakerClusterOldId").value(t.speakerClusterId)
                writer.name("startMs").value(t.startMs)
                writer.name("endMs").value(t.endMs)
                writer.endObject()

                afterId = t.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class SegmentSpeakerAssignmentBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/segment_speaker_assignments.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var offset = 0
        var count = 0L

        while (true) {
            val page = dao.backupSegmentSpeakerAssignments(750, offset)
            if (page.isEmpty()) break

            for (a in page) {
                writer.beginObject()
                writer.name("segmentOldId").value(a.segmentId)
                writer.name("speakerClusterOldId").value(a.speakerClusterId)
                writer.name("overlapRatio").value(a.overlapRatio.toDouble())
                writer.endObject()
                count++
            }

            offset += page.size
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class SpeakerDiarizationRunBackupExporter(
    private val dao: BackupDao
) : BackupSectionExporter {

    override val entryName = "data/speaker_diarization_runs.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.beginArray()

        var afterId = Long.MIN_VALUE
        var count = 0L

        while (true) {
            val page = dao.backupSpeakerDiarizationRunsAfter(afterId, 500)
            if (page.isEmpty()) break

            for (r in page) {
                writer.beginObject()
                writer.name("oldId").value(r.id)
                writer.name("transcriptOldId").value(r.transcriptId)
                writer.name("status").value(r.status)
                writer.name("progress").value(r.progress.toLong())
                writer.name("speakerCountMode").value(r.speakerCountMode)
                if (r.requestedSpeakerCount != null) {
                    writer.name("requestedSpeakerCount").value(r.requestedSpeakerCount.toLong())
                } else {
                    writer.name("requestedSpeakerCount").nullValue()
                }
                writer.name("detectedSpeakerCount").value(r.detectedSpeakerCount.toLong())
                writer.name("engineVersion").value(r.engineVersion)
                writer.name("segmentationModelId").value(r.segmentationModelId)
                writer.name("embeddingModelId").value(r.embeddingModelId)
                if (r.errorMessage != null) {
                    writer.name("errorMessage").value(r.errorMessage)
                } else {
                    writer.name("errorMessage").nullValue()
                }
                writer.name("createdAt").value(r.createdAt)
                writer.name("updatedAt").value(r.updatedAt)
                writer.endObject()

                afterId = r.id
                count++
            }
            writer.flush()
            onRows(count)
        }

        writer.endArray()
        writer.flush()
        onRows(count)
        return BackupSectionResult(count)
    }
}

class PortableSettingsExporter(
    private val settingsRepository: AppSettingsRepository
) : BackupSectionExporter {

    override val entryName = "settings/portable_settings.json"

    override suspend fun export(
        output: OutputStream,
        onRows: suspend (Long) -> Unit
    ): BackupSectionResult {
        val current = settingsRepository.settings.first()
        val writer = JsonWriter(OutputStreamWriter(output, Charsets.UTF_8))

        writer.beginObject()
        writer.name("languageCode").value(current.languageCode)
        writer.name("onboardingComplete").value(current.onboardingComplete)
        writer.endObject()

        writer.flush()
        onRows(1L)
        return BackupSectionResult(1L)
    }
}
