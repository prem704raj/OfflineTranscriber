package app.offlinetranscriber.mobile.backup.restore

import app.offlinetranscriber.mobile.data.database.AppDatabase

class UserContentClearer(
    private val database: AppDatabase
) {

    suspend fun clearForReplace() {
        val dao = database.backupDao()
        dao.deleteAllAskConversations()
        dao.deleteAllCollections()
        dao.deleteAllTranscriptionJobs()
        dao.deleteAllTranscripts()
    }
}
