package com.example.transcriber.export.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.example.transcriber.export.model.ExportArtifact

class ExportCreateDocumentContract : ActivityResultContract<ExportArtifact, Uri?>() {

    override fun createIntent(context: Context, input: ExportArtifact): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = input.mimeType
            putExtra(Intent.EXTRA_TITLE, input.fileName)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.data
        } else {
            null
        }
    }
}
