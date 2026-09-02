package com.example.transcriber.shortcuts

import android.content.Intent
import com.example.transcriber.billing.ProFeature
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ShortcutCommandRouter {

    private val _commands = MutableSharedFlow<ExternalAppCommand>(extraBufferCapacity = 4)
    val commands = _commands.asSharedFlow()

    fun handle(intent: Intent?) {
        val command = when (intent?.action) {
            ShortcutActions.RECORD -> ExternalAppCommand.Record
            ShortcutActions.IMPORT_AUDIO -> ExternalAppCommand.ImportAudio
            ShortcutActions.OPEN_QUEUE -> ExternalAppCommand.OpenQueue
            ShortcutActions.OPEN_MODELS -> ExternalAppCommand.OpenModels
            ShortcutActions.OPEN_PAYWALL -> {
                val raw = intent.getStringExtra(ShortcutActions.EXTRA_PRO_FEATURE)
                ExternalAppCommand.OpenPaywall(
                    feature = raw?.let {
                        runCatching { ProFeature.valueOf(it) }.getOrNull()
                    }
                )
            }
            else -> null
        }

        command?.let {
            _commands.tryEmit(it)
        }
    }
}
