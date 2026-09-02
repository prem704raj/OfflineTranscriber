package com.example.transcriber.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.transcriber.MainActivity
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.shortcuts.ShortcutActions
import com.example.transcriber.theme.TranscriberTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    private val state = MutableStateFlow<ShareImportUiState>(ShareImportUiState.Importing)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TranscriberTheme {
                ShareImportScreen(
                    state = state,
                    onContinue = { result ->
                        openMainAfterImport(result)
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                runCatching {
                    ShareImportCoordinator(this@ShareReceiverActivity).handle(intent)
                }.onSuccess {
                    state.value = ShareImportUiState.Done(it)
                }.onFailure {
                    state.value = ShareImportUiState.Error(
                        it.message ?: "Unable to import shared media."
                    )
                }
            }
        }
    }

    private fun openMainAfterImport(
        result: ShareImportResult
    ) {
        val target = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            when {
                result.needsModel -> {
                    action = ShortcutActions.OPEN_MODELS
                }

                result.videoProRejected > 0 && result.enqueuedCount == 0 -> {
                    action = ShortcutActions.OPEN_PAYWALL
                    putExtra(
                        ShortcutActions.EXTRA_PRO_FEATURE,
                        ProFeature.VIDEO_TRANSCRIPTION.name
                    )
                }

                else -> {
                    action = ShortcutActions.OPEN_QUEUE
                    putExtra(
                        ShortcutActions.EXTRA_SHARE_SUMMARY,
                        buildSummary(result)
                    )
                }
            }
        }

        startActivity(target)
        finish()
    }

    private fun buildSummary(
        result: ShareImportResult
    ): String = buildString {
        append("${result.enqueuedCount} added")

        if (result.totalRejected > 0) {
            append(" • ${result.totalRejected} not added")
        }

        if (result.videoProRejected > 0) {
            append(" • video requires Pro")
        }

        if (result.queueLimitRejected > 0) {
            append(" • queue limit reached")
        }
    }
}
