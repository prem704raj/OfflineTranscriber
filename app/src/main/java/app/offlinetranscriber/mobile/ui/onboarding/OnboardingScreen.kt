package app.offlinetranscriber.mobile.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.offlinetranscriber.mobile.modelmanager.ModelDownloadState
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.modelmanager.ModelManagerViewModel
import app.offlinetranscriber.mobile.ui.system.OtOperationStrip
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel
import app.offlinetranscriber.mobile.ui.system.OtWaveToTextMark

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

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.ScreenHorizontal, vertical = AppDimens.Space8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (page) {
                0 -> {
                    OtWaveToTextMark()

                    Spacer(Modifier.height(AppDimens.Space6))

                    Text(
                        "Private transcription",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(AppDimens.Space2))

                    Text(
                        "Core speech-to-text runs entirely on your phone. Your audio recordings never leave your device for transcription.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                1 -> {
                    OtStatusLabel(
                        text = "ON-DEVICE WHISPER",
                        kind = OtStatusKind.LOCAL
                    )

                    Spacer(Modifier.height(AppDimens.Space4))

                    Text(
                        "Choose your Whisper model",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(AppDimens.Space2))

                    Text(
                        "Recommended for this phone: ${recommended?.spec?.label ?: recommendedLabel}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(AppDimens.Space5))

                    if (recommended != null && !recommended.installed) {
                        val download = modelState.download

                        if (download is ModelDownloadState.Downloading && download.modelId == recommended.spec.id) {
                            val percent = download.fraction?.let { (it * 100).toInt() } ?: 0
                            OtOperationStrip(
                                title = "Downloading ${recommended.spec.label}",
                                progress = percent,
                                secondary = "Downloading offline Whisper model…",
                                onClick = null
                            )
                        } else {
                            Button(
                                onClick = {
                                    modelViewModel.download(recommended.spec)
                                },
                                shape = AppShapes.Button,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppDimens.PrimaryTouchTarget)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text(
                                    "Download ${recommended.spec.label} (${recommended.spec.formattedApproximateSize})",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    } else {
                        OtStatusLabel(
                            text = "Model is installed & ready!",
                            kind = OtStatusKind.LOCAL
                        )
                    }

                    Spacer(Modifier.height(AppDimens.Space3))

                    OutlinedButton(
                        onClick = onOpenModels,
                        shape = AppShapes.Button,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("See all models")
                    }
                }

                else -> {
                    Icon(
                        imageVector = OtIcons.LocalShield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(12.dp)
                    )

                    Spacer(Modifier.height(AppDimens.Space4))

                    Text(
                        "You're all set!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(AppDimens.Space2))

                    Text(
                        "Import audio, record lectures, or import videos for offline subtitle generation & study cards.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(AppDimens.Space8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (page > 0) {
                    OutlinedButton(
                        onClick = { page-- },
                        shape = AppShapes.Button,
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
                    shape = AppShapes.Button,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (page < 2) "Continue" else "Start transcribing")
                }
            }
        }
    }
}
