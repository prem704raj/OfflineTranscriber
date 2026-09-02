package com.example.transcriber.backup.restore

import com.example.transcriber.data.database.AppDatabase

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
