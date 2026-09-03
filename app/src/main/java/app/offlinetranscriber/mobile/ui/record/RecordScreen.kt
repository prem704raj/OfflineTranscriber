package app.offlinetranscriber.mobile.ui.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.offlinetranscriber.mobile.recorder.RecordingForegroundService
import app.offlinetranscriber.mobile.recorder.RecordingSessionStore
import app.offlinetranscriber.mobile.recorder.RecordingStatus
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
                title = { Text("Record Audio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Status and Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusText = when (recordingState.status) {
                    RecordingStatus.IDLE -> "Ready to Record"
                    RecordingStatus.RECORDING -> "Recording in Progress"
                    RecordingStatus.PAUSED -> "Recording Paused"
                    RecordingStatus.COMPLETED -> "Recording Complete"
                    RecordingStatus.ERROR -> recordingState.errorMessage ?: "Recording Error"
                }

                val statusColor by animateColorAsState(
                    targetValue = when (recordingState.status) {
                        RecordingStatus.RECORDING -> MaterialTheme.colorScheme.error
                        RecordingStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                        RecordingStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        RecordingStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "statusColor"
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = formatDuration(recordingState.durationMs),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 2. Waveform / Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedScale by animateFloatAsState(
                    targetValue = if (recordingState.status == RecordingStatus.RECORDING) {
                        1f + (recordingState.amplitude * 0.8f)
                    } else {
                        1f
                    },
                    animationSpec = spring(),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            if (recordingState.status == RecordingStatus.RECORDING) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            }
                        )
                )

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            if (recordingState.status == RecordingStatus.RECORDING) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = if (recordingState.status == RecordingStatus.RECORDING) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            // 3. Actions / Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    context.startService(RecordingForegroundService.pauseIntent(context))
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause")
                            }

                            IconButton(
                                onClick = {
                                    context.startService(RecordingForegroundService.stopIntent(context))
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp))
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
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    context.startService(RecordingForegroundService.resumeIntent(context))
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            }

                            IconButton(
                                onClick = {
                                    context.startService(RecordingForegroundService.stopIntent(context))
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    RecordingStatus.COMPLETED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Transcribe Audio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    showDiscardConfirm = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
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
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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
