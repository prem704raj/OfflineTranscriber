package com.example.transcriber.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcriber.modelmanager.ModelDownloadState
import com.example.transcriber.ui.modelmanager.ModelManagerViewModel

@Composable
fun OnboardingScreen(
    recommendedLabel: String,
    onFinish: () -> Unit,
    onOpenModels: () -> Unit,
    modelViewModel: ModelManagerViewModel = viewModel()
) {
    var page by remember { mutableIntStateOf(0) }

    val modelState by modelViewModel.state.collectAsState()

    val recommended = modelState.rows.firstOrNull { it.recommended }
        ?: modelState.rows.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(48.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Private transcription",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Core speech-to-text runs entirely on your phone. Your audio recordings never leave your device for transcription.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            1 -> {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(48.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Choose your Whisper model",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recommended for this phone: ${recommended?.spec?.label ?: recommendedLabel}",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                if (recommended != null && !recommended.installed) {
                    val download = modelState.download

                    if (download is ModelDownloadState.Downloading && download.modelId == recommended.spec.id) {
                        download.fraction?.let { fraction ->
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } ?: LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Downloading model…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = {
                                modelViewModel.download(recommended.spec)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, null)
                            Spacer(Modifier.padding(4.dp))
                            Text("Download ${recommended.spec.label} (${recommended.spec.formattedApproximateSize})")
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.padding(6.dp))
                            Text(
                                "Model is installed & ready!",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenModels,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("See all models")
                }
            }

            else -> {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(24.dp)
                            .size(48.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "You're all set!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Import audio, record lectures, or import videos for offline subtitle generation & study cards.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (page > 0) {
                OutlinedButton(
                    onClick = { page-- },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (page < 2) {
                        page++
                    } else {
                        onFinish()
                    }
                },
                enabled = page != 1 || modelState.rows.any { it.installed },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (page < 2) "Continue" else "Start transcribing")
            }
        }
    }
}
