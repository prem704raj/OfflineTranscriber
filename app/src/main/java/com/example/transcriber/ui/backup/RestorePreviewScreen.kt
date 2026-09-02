package com.example.transcriber.ui.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.backup.RestoreMode
import com.example.transcriber.backup.RestorePreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestorePreviewScreen(
    viewModel: BackupViewModel,
    preview: RestorePreview,
    onBack: () -> Unit
) {
    val selectedMode by viewModel.selectedRestoreMode.collectAsState()
    val restoreSettings by viewModel.restoreSettingsOption.collectAsState()
    val showReplaceDialog by viewModel.showReplaceConfirmDialog.collectAsState()

    if (showReplaceDialog) {
        ReplaceConfirmationDialog(
            onConfirm = { viewModel.confirmAndExecuteRestore() },
            onDismiss = { viewModel.dismissReplaceDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Metadata card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (preview.encrypted) Icons.Default.Lock else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (preview.encrypted) "Encrypted Backup (Verified)" else "Valid Offline Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(preview.createdAtEpochMs))
                            Text(
                                text = "Created $dateStr • App v${preview.appVersionName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Backup Contents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow(Icons.Default.RecordVoiceOver, "Transcripts", "${preview.transcripts}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(Icons.Default.Folder, "Collections", "${preview.collections}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(Icons.Default.School, "Study Packs", "${preview.studyPacks}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(Icons.Default.Groups, "Meeting Packs", "${preview.meetingPacks}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    StatRow(Icons.Default.Forum, "Ask Conversations", "${preview.askConversations}")

                    if (preview.mediaCount > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        val mb = preview.mediaBytes / (1024L * 1024L)
                        StatRow(Icons.Default.Movie, "Media Files", "${preview.mediaCount} (~${mb} MB)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Restore Strategy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector: Merge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMode == RestoreMode.MERGE,
                        onClick = { viewModel.setRestoreMode(RestoreMode.MERGE) }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    RadioButton(
                        selected = selectedMode == RestoreMode.MERGE,
                        onClick = { viewModel.setRestoreMode(RestoreMode.MERGE) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merge with existing library (Recommended)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Adds new transcripts and merges collections. Skips exact duplicate transcripts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector: Replace
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedMode == RestoreMode.REPLACE,
                        onClick = { viewModel.setRestoreMode(RestoreMode.REPLACE) }
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    RadioButton(
                        selected = selectedMode == RestoreMode.REPLACE,
                        onClick = { viewModel.setRestoreMode(RestoreMode.REPLACE) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replace entire library",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Deletes all current transcripts on this device and restores from backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (preview.includesSettings) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restore App Preferences",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Restores backed-up language preference.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = restoreSettings,
                            onCheckedChange = { viewModel.setRestoreSettingsOption(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.requestRestoreExecution() },
                colors = if (selectedMode == RestoreMode.REPLACE) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedMode == RestoreMode.REPLACE) "Confirm & Replace Library" else "Restore & Merge Library")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
