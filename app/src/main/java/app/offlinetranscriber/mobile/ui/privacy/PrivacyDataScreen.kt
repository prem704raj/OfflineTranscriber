package app.offlinetranscriber.mobile.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    onBack: () -> Unit,
    onDiagnostics: () -> Unit,
    viewModel: PrivacyDataViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var confirmTranscripts by remember { mutableStateOf(false) }
    var confirmModels by remember { mutableStateOf(false) }
    var confirmAll by remember { mutableStateOf(false) }
    var typedDelete by remember { mutableStateOf("") }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Data") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.padding(7.dp))
                    Column {
                        Text(
                            "Private by design",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Core transcription runs on this device. Your audio is never uploaded to any server for transcription.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.storage?.let { storage ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Local storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Models: ${formatBytes(storage.modelsBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Recordings: ${formatBytes(storage.recordingsBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Extracted media: ${formatBytes(storage.extractedAudioBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Temporary: ${formatBytes(storage.temporaryBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            ActionButton(
                text = "Clean temporary files",
                icon = Icons.Default.CleaningServices,
                destructive = false,
                enabled = !state.busy,
                onClick = viewModel::cleanTemp
            )

            ActionButton(
                text = "Delete transcription content",
                icon = Icons.Default.DeleteSweep,
                destructive = true,
                enabled = !state.busy,
                onClick = { confirmTranscripts = true }
            )

            ActionButton(
                text = "Delete downloaded models",
                icon = Icons.Default.Memory,
                destructive = true,
                enabled = !state.busy,
                onClick = { confirmModels = true }
            )

            ActionButton(
                text = "Reset preferences",
                icon = Icons.Default.RestartAlt,
                destructive = false,
                enabled = !state.busy,
                onClick = viewModel::resetPreferences
            )

            OutlinedButton(
                onClick = onDiagnostics,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View local diagnostics")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    typedDelete = ""
                    confirmAll = true
                },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null
                )
                Spacer(Modifier.padding(4.dp))
                Text("Delete all local app data")
            }
        }
    }

    if (confirmTranscripts) {
        SimpleConfirmation(
            title = "Delete transcription content?",
            body = "This removes local transcripts, bookmarks, collections, study packs, queue history, app recordings and extracted media. Installed models and Pro ownership cache are kept.",
            confirm = "Delete content",
            onDismiss = { confirmTranscripts = false },
            onConfirm = {
                confirmTranscripts = false
                viewModel.deleteTranscriptionContent()
            }
        )
    }

    if (confirmModels) {
        SimpleConfirmation(
            title = "Delete downloaded models?",
            body = "You will need to download a Whisper model again before new transcription can run.",
            confirm = "Delete models",
            onDismiss = { confirmModels = false },
            onConfirm = {
                confirmModels = false
                viewModel.deleteModels()
            }
        )
    }

    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Delete all local app data?") },
            text = {
                Column {
                    Text(
                        "This removes transcripts, recordings, study data, collections, queue history, models, preferences and local caches. This cannot be undone."
                    )

                    Spacer(Modifier.height(14.dp))

                    Text("Type DELETE to confirm:")

                    Spacer(Modifier.height(6.dp))

                    OutlinedTextField(
                        value = typedDelete,
                        onValueChange = { typedDelete = it.take(12) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllLocalData(typedDelete)
                        if (typedDelete == "DELETE") {
                            confirmAll = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = (typedDelete == "DELETE")
                ) {
                    Text("Delete everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (destructive) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text(text)
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text(text)
        }
    }
}

@Composable
private fun SimpleConfirmation(
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBytes(
    bytes: Long
): String {
    if (bytes < 1024L) {
        return "$bytes B"
    }

    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1

    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }

    return String.format(
        Locale.US,
        "%.1f %s",
        value,
        units[index]
    )
}
