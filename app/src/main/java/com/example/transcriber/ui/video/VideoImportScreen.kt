package com.example.transcriber.ui.video

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.transcriber.video.VideoImportViewModel
import com.example.transcriber.video.VideoPreparationState

@UnstableApi
@Composable
fun VideoImportScreen(
    selectedVideoUri: Uri,
    onPrepared: (sourceVideoUri: Uri, extractedAudioUri: Uri, displayName: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: VideoImportViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(selectedVideoUri) {
        viewModel.prepareVideo(selectedVideoUri)
    }

    LaunchedEffect(state) {
        val ready = state as? VideoPreparationState.Ready ?: return@LaunchedEffect
        onPrepared(ready.sourceVideoUri, ready.extractedAudioUri, ready.displayName)
        viewModel.reset()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(20.dp))

            when (val current = state) {
                VideoPreparationState.Idle -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Preparing video…", style = MaterialTheme.typography.bodyLarge)
                }

                is VideoPreparationState.Preparing -> {
                    Text(
                        "Extracting Audio",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        current.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))

                    if (current.progressPercent != null) {
                        LinearProgressIndicator(
                            progress = { current.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${current.progressPercent}% • Extracting audio locally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Extracting audio locally…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.cancel()
                            onCancel()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }

                is VideoPreparationState.Ready -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(14.dp))
                    Text("Starting offline transcription…", style = MaterialTheme.typography.bodyLarge)
                }

                is VideoPreparationState.Error -> {
                    Text(
                        "Video preparation failed",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        current.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.prepareVideo(selectedVideoUri) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try again")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.cancel()
                            onCancel()
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
