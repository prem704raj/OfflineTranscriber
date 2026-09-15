package app.offlinetranscriber.mobile.ui.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.offlinetranscriber.mobile.recorder.RecordingForegroundService
import app.offlinetranscriber.mobile.recorder.RecordingSessionStore
import app.offlinetranscriber.mobile.recorder.RecordingStatus
import app.offlinetranscriber.mobile.theme.OtDisplayFamily
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtAmplitudeMeter
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onTranscribe: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by RecordingSessionStore.state.collectAsState()
    var showDiscardConfirm by remember { mutableStateOf(false) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            startRecordingService(context)
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard Recording?") },
            text = { Text("This recording will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        context.startService(RecordingForegroundService.discardIntent(context))
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Record Audio",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.accessibleAction("Back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { padding ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppDimens.ScreenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(AppDimens.Space6))

                // 1. Status and Tabular Timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (statusText, statusKind) = when (recordingState.status) {
                        RecordingStatus.IDLE -> "Ready to Record" to OtStatusKind.NEUTRAL
                        RecordingStatus.RECORDING -> "Recording in Progress" to OtStatusKind.PROCESSING
                        RecordingStatus.PAUSED -> "Recording Paused" to OtStatusKind.NEUTRAL
                        RecordingStatus.COMPLETED -> "Recording Complete" to OtStatusKind.LOCAL
                        RecordingStatus.ERROR -> (recordingState.errorMessage ?: "Recording Error") to OtStatusKind.NEUTRAL
                    }

                    OtStatusLabel(
                        text = statusText,
                        kind = statusKind
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Space4))

                    Text(
                        text = formatDuration(recordingState.durationMs),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = OtDisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 54.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Physical Waveform Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.Space4),
                    contentAlignment = Alignment.Center
                ) {
                    val rawAmp = (recordingState.amplitude * 32767f).toInt()
                    OtAmplitudeMeter(
                        amplitude = if (recordingState.status == RecordingStatus.RECORDING) rawAmp else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    )
                }

                // 3. Physical Transport Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.Space8),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (recordingState.status) {
                        RecordingStatus.IDLE -> {
                            Button(
                                onClick = {
                                    if (hasMicPermission) {
                                        startRecordingService(context)
                                    } else {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppDimens.PrimaryTouchTarget),
                                shape = AppShapes.Button
                            ) {
                                Icon(
                                    imageVector = OtIcons.RecordWave,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Start Recording",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        RecordingStatus.RECORDING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showDiscardConfirm = true },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .accessibleAction("Discard recording")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        context.startService(RecordingForegroundService.pauseIntent(context))
                                    },
                                    modifier = Modifier
                                        .size(62.dp)
                                        .accessibleAction("Pause recording")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                }

                                IconButton(
                                    onClick = {
                                        context.startService(RecordingForegroundService.stopIntent(context))
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .accessibleAction("Stop recording"),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        RecordingStatus.PAUSED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = { showDiscardConfirm = true },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .accessibleAction("Discard recording")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        context.startService(RecordingForegroundService.resumeIntent(context))
                                    },
                                    modifier = Modifier
                                        .size(62.dp)
                                        .accessibleAction("Resume recording")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                }

                                IconButton(
                                    onClick = {
                                        context.startService(RecordingForegroundService.stopIntent(context))
                                    },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                        .accessibleAction("Stop recording"),
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        RecordingStatus.COMPLETED -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
                            ) {
                                Button(
                                    onClick = {
                                        recordingState.filePath?.let { path ->
                                            val file = File(path)
                                            if (file.exists()) {
                                                onTranscribe(file)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                    .height(AppDimens.PrimaryTouchTarget),
                                    shape = AppShapes.Button
                                ) {
                                    Icon(OtIcons.Transcript, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Transcribe Audio",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        showDiscardConfirm = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = AppShapes.Button
                                ) {
                                    Text("Discard Recording")
                                }
                            }
                        }

                        RecordingStatus.ERROR -> {
                            Button(
                                onClick = {
                                    RecordingSessionStore.reset()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.Button
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startRecordingService(context: Context) {
    val intent = RecordingForegroundService.startIntent(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000L
    val seconds = totalSec % 60
    val minutes = (totalSec / 60) % 60
    val hours = totalSec / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
