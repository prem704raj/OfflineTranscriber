package app.offlinetranscriber.mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.offlinetranscriber.mobile.data.model.AskCitationEntity
import app.offlinetranscriber.mobile.data.model.AskConversationEntity
import app.offlinetranscriber.mobile.data.model.AskMessageEntity
import app.offlinetranscriber.mobile.data.model.BookmarkEntity
import app.offlinetranscriber.mobile.data.model.CollectionEntity
import app.offlinetranscriber.mobile.data.model.FlashcardEntity
import app.offlinetranscriber.mobile.data.model.QuizQuestionEntity
import app.offlinetranscriber.mobile.data.model.StudyChapterEntity
import app.offlinetranscriber.mobile.data.model.StudyKeyPointEntity
import app.offlinetranscriber.mobile.data.model.StudyPackEntity
import app.offlinetranscriber.mobile.data.model.TranscriptCollectionCrossRef
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentEntity
import app.offlinetranscriber.mobile.data.model.TranscriptSegmentFts
import app.offlinetranscriber.mobile.data.model.MeetingActionEntity
import app.offlinetranscriber.mobile.data.model.MeetingDecisionEntity
import app.offlinetranscriber.mobile.data.model.MeetingPackEntity
import app.offlinetranscriber.mobile.data.model.MeetingQuestionEntity
import app.offlinetranscriber.mobile.data.model.MeetingSummaryCitationEntity
import app.offlinetranscriber.mobile.data.model.MeetingTopicEntity
import app.offlinetranscriber.mobile.data.model.SpeakerDiarizationRunEntity
import app.offlinetranscriber.mobile.data.model.SpeakerClusterEntity
import app.offlinetranscriber.mobile.data.model.SpeakerTurnEntity
import app.offlinetranscriber.mobile.data.model.SegmentSpeakerAssignmentEntity
import app.offlinetranscriber.mobile.queue.TranscriptionJobDao
import app.offlinetranscriber.mobile.queue.TranscriptionJobEntity
import org.json.JSONArray

@Database(
    entities = [
        TranscriptEntity::class,
        TranscriptSegmentEntity::class,
        TranscriptSegmentFts::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        TranscriptCollectionCrossRef::class,
        StudyPackEntity::class,
        StudyChapterEntity::class,
        StudyKeyPointEntity::class,
        FlashcardEntity::class,
        QuizQuestionEntity::class,
        TranscriptionJobEntity::class,
        AskConversationEntity::class,
        AskMessageEntity::class,
        AskCitationEntity::class,
        MeetingPackEntity::class,
        MeetingSummaryCitationEntity::class,
        MeetingActionEntity::class,
        MeetingDecisionEntity::class,
        MeetingQuestionEntity::class,
        MeetingTopicEntity::class,
        SpeakerDiarizationRunEntity::class,
        SpeakerClusterEntity::class,
        SpeakerTurnEntity::class,
        SegmentSpeakerAssignmentEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transcriptDao(): TranscriptDao
    abstract fun searchDao(): SearchDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun collectionDao(): CollectionDao
    abstract fun studyDao(): StudyDao
    abstract fun transcriptionJobDao(): TranscriptionJobDao
    abstract fun diagnosticsDao(): app.offlinetranscriber.mobile.diagnostics.DiagnosticsDao
    abstract fun privacyDataDao(): app.offlinetranscriber.mobile.privacy.PrivacyDataDao
    abstract fun askDao(): AskDao
    abstract fun meetingDao(): MeetingDao
    abstract fun speakerDiarizationDao(): SpeakerDiarizationDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcripts ADD COLUMN sourceUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transcripts ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'AUDIO'")
                db.execSQL("UPDATE transcripts SET sourceUri = COALESCE(audioUriString, '') WHERE sourceUri = ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create transcript_segments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcript_segments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transcript_segments_transcriptId` ON `transcript_segments` (`transcriptId`)"
                )

                // 2. Backfill existing segments from transcripts.segmentsJson
                try {
                    val cursor = db.query("SELECT id, segmentsJson FROM transcripts")
                    val transcriptSegments = mutableListOf<Triple<Long, Long, Pair<Long, String>>>()

                    while (cursor.moveToNext()) {
                        val transcriptId = cursor.getLong(0)
                        val jsonStr = cursor.getString(1)
                        if (!jsonStr.isNullOrBlank()) {
                            try {
                                val array = JSONArray(jsonStr)
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val startMs = obj.optLong("startMs", 0L)
                                    val endMs = obj.optLong("endMs", 0L)
                                    val text = obj.optString("text", "")
                                    if (text.isNotBlank()) {
                                        transcriptSegments.add(Triple(transcriptId, startMs, Pair(endMs, text)))
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    cursor.close()

                    for (seg in transcriptSegments) {
                        val transcriptId = seg.first
                        val startMs = seg.second
                        val endMs = seg.third.first
                        val text = seg.third.second.replace("'", "''")
                        db.execSQL(
                            "INSERT INTO `transcript_segments` (`transcriptId`, `startMs`, `endMs`, `text`) VALUES ($transcriptId, $startMs, $endMs, '$text')"
                        )
                    }
                } catch (_: Exception) {}

                // 3. Create FTS4 virtual table with unicode61 tokenizer
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `transcript_segments_fts`
                    USING FTS4(
                        `text`,
                        content=`transcript_segments`,
                        tokenize=unicode61
                    )
                    """.trimIndent()
                )

                // 4. Rebuild FTS table from existing segment rows
                db.execSQL(
                    "INSERT INTO transcript_segments_fts(transcript_segments_fts) VALUES('rebuild')"
                )

                // 5. Create bookmarks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `segmentId` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`segmentId`) REFERENCES `transcript_segments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_segmentId` ON `bookmarks` (`segmentId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bookmarks_transcriptId` ON `bookmarks` (`transcriptId`)"
                )

                // 6. Create collections table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 7. Create transcript_collection_cross_ref table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcript_collection_cross_ref` (
                        `transcriptId` INTEGER NOT NULL,
                        `collectionId` INTEGER NOT NULL,
                        PRIMARY KEY(`transcriptId`, `collectionId`),
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transcript_collection_cross_ref_collectionId` ON `transcript_collection_cross_ref` (`collectionId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_packs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `engine` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`)
                            REFERENCES `transcripts`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_study_packs_transcriptId` ON `study_packs` (`transcriptId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_chapters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studyPackId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        FOREIGN KEY(`studyPackId`)
                            REFERENCES `study_packs`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_chapters_studyPackId` ON `study_chapters` (`studyPackId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_key_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studyPackId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        FOREIGN KEY(`studyPackId`)
                            REFERENCES `study_packs`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_key_points_studyPackId` ON `study_key_points` (`studyPackId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flashcards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studyPackId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `front` TEXT NOT NULL,
                        `back` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        FOREIGN KEY(`studyPackId`)
                            REFERENCES `study_packs`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_flashcards_studyPackId` ON `flashcards` (`studyPackId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quiz_questions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studyPackId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `question` TEXT NOT NULL,
                        `optionA` TEXT NOT NULL,
                        `optionB` TEXT NOT NULL,
                        `optionC` TEXT NOT NULL,
                        `optionD` TEXT NOT NULL,
                        `correctIndex` INTEGER NOT NULL,
                        `explanation` TEXT NOT NULL,
                        FOREIGN KEY(`studyPackId`)
                            REFERENCES `study_packs`(`id`)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quiz_questions_studyPackId` ON `quiz_questions` (`studyPackId`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcription_jobs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `inputUri` TEXT NOT NULL,
                        `sourceUri` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `languageCode` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `resultTranscriptId` INTEGER,
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transcription_jobs_status` ON `transcription_jobs` (`status`)"
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transcription_jobs_createdAt` ON `transcription_jobs` (`createdAt`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE transcription_jobs
                    ADD COLUMN preparedInputUri TEXT
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE transcription_jobs
                    ADD COLUMN stage TEXT NOT NULL
                    DEFAULT 'TRANSCRIBING'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    UPDATE transcription_jobs
                    SET preparedInputUri = inputUri
                    WHERE preparedInputUri IS NULL
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ask_conversations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `scope` TEXT NOT NULL,
                        `transcriptId` INTEGER,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_conversations_transcriptId` ON `ask_conversations` (`transcriptId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_conversations_updatedAt` ON `ask_conversations` (`updatedAt`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ask_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER NOT NULL,
                        `role` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `engine` TEXT,
                        `insufficientEvidence` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `ask_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_messages_conversationId` ON `ask_messages` (`conversationId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_messages_createdAt` ON `ask_messages` (`createdAt`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ask_citations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `segmentId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        FOREIGN KEY(`messageId`) REFERENCES `ask_messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`segmentId`) REFERENCES `transcript_segments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_citations_messageId` ON `ask_citations` (`messageId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ask_citations_segmentId` ON `ask_citations` (`segmentId`)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_packs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `summary` TEXT NOT NULL,
                        `engine` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_meeting_packs_transcriptId` ON `meeting_packs` (`transcriptId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_packs_updatedAt` ON `meeting_packs` (`updatedAt`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_summary_citations` (
                        `meetingPackId` INTEGER NOT NULL,
                        `segmentId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`meetingPackId`, `segmentId`),
                        FOREIGN KEY(`meetingPackId`) REFERENCES `meeting_packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`segmentId`) REFERENCES `transcript_segments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_summary_citations_meetingPackId` ON `meeting_summary_citations` (`meetingPackId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_summary_citations_segmentId` ON `meeting_summary_citations` (`segmentId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_actions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `meetingPackId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `assignee` TEXT NOT NULL,
                        `dueText` TEXT NOT NULL,
                        `sourceSegmentId` INTEGER,
                        `startMs` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `manuallyEdited` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`meetingPackId`) REFERENCES `meeting_packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_actions_meetingPackId` ON `meeting_actions` (`meetingPackId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_actions_status` ON `meeting_actions` (`status`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_actions_startMs` ON `meeting_actions` (`startMs`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_decisions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `meetingPackId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `sourceSegmentId` INTEGER,
                        `startMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`meetingPackId`) REFERENCES `meeting_packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_decisions_meetingPackId` ON `meeting_decisions` (`meetingPackId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_decisions_startMs` ON `meeting_decisions` (`startMs`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_questions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `meetingPackId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `sourceSegmentId` INTEGER,
                        `startMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`meetingPackId`) REFERENCES `meeting_packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_questions_meetingPackId` ON `meeting_questions` (`meetingPackId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_questions_startMs` ON `meeting_questions` (`startMs`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `meeting_topics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `meetingPackId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `sourceSegmentId` INTEGER,
                        `startMs` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`meetingPackId`) REFERENCES `meeting_packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_topics_meetingPackId` ON `meeting_topics` (`meetingPackId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_meeting_topics_startMs` ON `meeting_topics` (`startMs`)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `speaker_diarization_runs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `speakerCountMode` TEXT NOT NULL,
                        `requestedSpeakerCount` INTEGER,
                        `detectedSpeakerCount` INTEGER NOT NULL,
                        `engineVersion` TEXT NOT NULL,
                        `segmentationModelId` TEXT NOT NULL,
                        `embeddingModelId` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_speaker_diarization_runs_transcriptId` ON `speaker_diarization_runs` (`transcriptId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_diarization_runs_status` ON `speaker_diarization_runs` (`status`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_diarization_runs_updatedAt` ON `speaker_diarization_runs` (`updatedAt`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `speaker_clusters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `speakerIndex` INTEGER NOT NULL,
                        `customName` TEXT NOT NULL,
                        `userNamed` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_clusters_transcriptId` ON `speaker_clusters` (`transcriptId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_speaker_clusters_transcriptId_speakerIndex` ON `speaker_clusters` (`transcriptId`, `speakerIndex`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `speaker_turns` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcriptId` INTEGER NOT NULL,
                        `speakerClusterId` INTEGER NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        FOREIGN KEY(`transcriptId`) REFERENCES `transcripts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`speakerClusterId`) REFERENCES `speaker_clusters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_turns_transcriptId` ON `speaker_turns` (`transcriptId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_turns_speakerClusterId` ON `speaker_turns` (`speakerClusterId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_speaker_turns_startMs` ON `speaker_turns` (`startMs`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `segment_speaker_assignments` (
                        `segmentId` INTEGER NOT NULL,
                        `speakerClusterId` INTEGER NOT NULL,
                        `overlapRatio` REAL NOT NULL,
                        PRIMARY KEY(`segmentId`),
                        FOREIGN KEY(`segmentId`) REFERENCES `transcript_segments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`speakerClusterId`) REFERENCES `speaker_clusters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_segment_speaker_assignments_speakerClusterId` ON `segment_speaker_assignments` (`speakerClusterId`)"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transcription_jobs ADD COLUMN preparedPcmPath TEXT DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE transcription_jobs ADD COLUMN checkpointSample INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE transcription_jobs ADD COLUMN partialSegmentsJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transcriber_database.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
