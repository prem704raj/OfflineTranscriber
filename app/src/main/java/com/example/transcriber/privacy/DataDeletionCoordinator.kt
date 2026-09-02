package com.example.transcriber.privacy

import android.content.Context
import androidx.room.withTransaction
import com.example.transcriber.data.database.AppDatabase
import com.example.transcriber.modelmanager.ModelManager
import com.example.transcriber.settings.AppSettingsRepository
import com.example.transcriber.storage.StorageManager
import java.io.File

class DataDeletionCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val privacyDao: PrivacyDataDao,
    private val modelManager: ModelManager,
    private val settings: AppSettingsRepository,
    private val storageManager: StorageManager
) {

    suspend fun cleanTemporaryFiles() {
        storageManager.cleanTemporaryFiles()
    }

    suspend fun deleteTranscriptionContent() {
        database.withTransaction {
            privacyDao.deleteTranscriptionContent()
        }

        deleteAppOwnedRecordings()
        deleteAppOwnedExtractedMedia()
        deleteSharedImports()
    }

    suspend fun deleteAllModels() {
        val installed = modelManager.installedModels().toList()

        installed.forEach { spec ->
            modelManager.modelFile(spec).delete()

            val parent = modelManager.modelFile(spec).parentFile
            if (parent != null) {
                File(parent, spec.fileName + ".part").delete()
            }
        }

        settings.setSelectedModel("")
    }

    suspend fun resetPreferences() {
        settings.setSelectedModel("")
        settings.setLanguage("auto")
        settings.setOnboardingComplete(false)
    }

    suspend fun deleteEverythingExceptBillingCache() {
        deleteTranscriptionContent()
        deleteAllModels()
        cleanTemporaryFiles()
        resetPreferences()
    }

    private fun deleteAppOwnedRecordings() {
        File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
            "recordings"
        ).deleteRecursively()
    }

    private fun deleteAppOwnedExtractedMedia() {
        File(
            context.getExternalFilesDir(null),
            "video_audio"
        ).deleteRecursively()
    }

    private fun deleteSharedImports() {
        File(
            context.filesDir,
            "shared_imports"
        ).deleteRecursively()
    }
}
