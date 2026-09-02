package com.example.transcriber

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.transcriber.background.BackgroundTranscriptionStarter
import com.example.transcriber.theme.TranscriberTheme
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as TranscriberApplication
        app.shortcutCommandRouter.handle(intent)

        enableEdgeToEdge()
        setContent {
            TranscriberTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val app = application as TranscriberApplication
        app.shortcutCommandRouter.handle(intent)
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            BackgroundTranscriptionStarter.resumeIfNeededFromVisibleUi(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as TranscriberApplication
        app.billingRepository.connect()
        lifecycleScope.launch {
            app.billingRepository.refreshAll()
        }
    }
}
