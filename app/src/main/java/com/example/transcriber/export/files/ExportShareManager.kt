package com.example.transcriber.export.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.transcriber.export.model.ExportArtifact
import java.io.File

class ExportShareManager(
    private val context: Context,
    private val cacheManager: ExportCacheManager
) {

    fun createShareIntent(artifact: ExportArtifact): Intent {
        val sourceFile = File(artifact.filePath)
        val stagedFile = cacheManager.stageForShare(sourceFile, artifact.fileName)

        val authority = "${context.packageName}.files"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, stagedFile)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = artifact.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(sendIntent, "Share Export").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
