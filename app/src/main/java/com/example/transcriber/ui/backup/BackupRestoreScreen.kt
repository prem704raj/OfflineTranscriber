package com.example.transcriber.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.backup.BackupOperationStatus
import com.example.transcriber.backup.BackupOperationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    onCreateBackupClick: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onBack: () -> Unit
) {
    val operationState by viewModel.operationState.collectAsState()
    val lastInspection by viewModel.lastInspection.collectAsState()
    val lastRestoreResult by viewModel.lastRestoreResult.collectAsState()
    val showPasswordSheet by viewModel.showPasswordSheet.collectAsState()
    val inspectPasswordInput by viewModel.inspectPasswordInput.collectAsState()
    val inspectPasswordError by viewModel.inspectPasswordError.collectAsState()

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onRestoreFileSelected(uri)
        }
    }

    LaunchedEffect(operationState?.status) {
        if (operationState?.status == BackupOperationStatus.READY_FOR_RESTORE) {
            onNavigateToPreview()
        }
    }

    if (showPasswordSheet) {
        RestorePasswordSheet(
            passwordInput = inspectPasswordInput,
            errorMessage = inspectPasswordError,
            onPasswordChange = { viewModel.setInspectPasswordInput(it) },
            onSubmit = { viewModel.submitInspectPassword() },
            onDismiss = { viewModel.dismissPasswordSheet() }
        )
    }

    val state = operationState
    if (state != null && (state.status == BackupOperationStatus.RUNNING || state.status == BackupOperationStatus.QUEUED || state.status == BackupOperationStatus.FAILED || state.status == BackupOperationStatus.CANCELLED)) {
        BackupProgressScreen(
            state = state,
            onCancel = { viewModel.cancelActiveOperation() },
            onDismiss = { viewModel.clearOperationState() }
        )
        return
    }

    val restoreResult = lastRestoreResult
    if (restoreResult != null && state?.status == BackupOperationStatus.COMPLETED && state.type == BackupOperationType.RESTORE) {
        RestoreResultScreen(
            result = restoreResult,
            onDone = {
                viewModel.clearOperationState()
                onBack()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
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

            // Privacy & offline banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Backups are stored locally on your device or preferred storage. No cloud accounts required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Create Backup
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Export your entire library of transcripts, study packs, AI ask chats, meeting actions, and speaker profiles into a single portable .otbackup file. Includes optional AES-256-GCM password encryption and media archiving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.loadEstimate()
                            onCreateBackupClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Backup")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Restore Backup
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Restore Backup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select a .otbackup file to inspect its contents and safely restore into your library. You can choose to merge new transcripts without losing existing content or perform a clean library replacement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup File to Restore")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
