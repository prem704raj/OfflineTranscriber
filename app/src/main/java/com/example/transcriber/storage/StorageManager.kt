package com.example.transcriber.storage

import android.content.Context
import com.example.transcriber.modelmanager.ModelManager
import java.io.File

class StorageManager(
    private val context: Context,
    private val modelManager: ModelManager
) {
    fun usage(): StorageUsage {
        return StorageUsage(
            modelsBytes = modelManager.totalInstalledBytes(),
            recordingsBytes = sizeOf(
                File(
                    context.getExternalFilesDir(
                        android.os.Environment.DIRECTORY_MUSIC
                    ),
                    "recordings"
                )
            ),
            extractedAudioBytes = sizeOf(
                File(
                    context.getExternalFilesDir(null),
                    "video_audio"
                )
            ),
            temporaryBytes = sizeOf(
                File(
                    context.cacheDir,
                    "shared_subtitles"
                )
            ) + sizeOf(context.externalCacheDir) + sizeOf(context.cacheDir)
        )
    }

    fun cleanTemporaryFiles() {
        File(
            context.cacheDir,
            "shared_subtitles"
        ).deleteRecursively()

        File(
            context.cacheDir,
            "export_work"
        ).deleteRecursively()

        File(
            context.cacheDir,
            "export_share"
        ).deleteRecursively()

        File(
            context.cacheDir,
            "caption_video_export"
        ).deleteRecursively()

        File(
            context.cacheDir,
            "shared_caption_videos"
        ).deleteRecursively()

        context.externalCacheDir?.deleteRecursively()

        // Clean temp cache files in internal cacheDir (excluding vital structures)
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tmp") || file.name.endsWith(".part") || file.name.startsWith("temp_")) {
                file.deleteRecursively()
            }
        }

        // Do NOT delete:
        // - recordings
        // - transcript DB
        // - source files
        // - installed Whisper models
        // - current extracted audio referenced by video transcripts
    }

    private fun sizeOf(
        file: File?
    ): Long {
        if (
            file == null ||
            !file.exists()
        ) return 0L

        if (file.isFile) {
            return file.length()
        }

        return file.listFiles()
            ?.sumOf(::sizeOf)
            ?: 0L
    }
}
