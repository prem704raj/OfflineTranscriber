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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.transcriber.backup.RestoreMode
import com.example.transcriber.backup.RestoreResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreResultScreen(
    result: RestoreResult,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore Completed") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (result.mode == RestoreMode.MERGE) "Library Merged Successfully" else "Library Replaced Successfully",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ResultRow(
                        icon = Icons.Default.RecordVoiceOver,
                        label = "New Transcripts",
                        value = "${result.importedTranscripts}"
                    )

                    if (result.mode == RestoreMode.MERGE && result.reusedDuplicateTranscripts > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ResultRow(
                            icon = Icons.Default.RecordVoiceOver,
                            label = "Duplicate Transcripts Preserved",
                            value = "${result.reusedDuplicateTranscripts}"
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(
                        icon = Icons.Default.Folder,
                        label = "Collections Restored",
                        value = "${result.restoredCollections}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(
                        icon = Icons.Default.School,
                        label = "Study Packs Restored",
                        value = "${result.restoredStudyPacks}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(
                        icon = Icons.Default.Groups,
                        label = "Meeting Packs Restored",
                        value = "${result.restoredMeetingPacks}"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow(
                        icon = Icons.Default.Forum,
                        label = "Ask Conversations Restored",
                        value = "${result.restoredAskConversations}"
                    )

                    if (result.restoredMediaFiles > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ResultRow(
                            icon = Icons.Default.Movie,
                            label = "Media Files Restored",
                            value = "${result.restoredMediaFiles}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Library")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultRow(
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
