package app.offlinetranscriber.mobile.backup.restore

import app.offlinetranscriber.mobile.data.database.AppDatabase

class TranscriptFtsRebuilder(
    private val database: AppDatabase
) {

    fun rebuild() {
        runCatching {
            database.openHelper.writableDatabase.execSQL(
                "INSERT INTO transcript_segments_fts(transcript_segments_fts) VALUES('rebuild')"
            )
        }
    }
}
