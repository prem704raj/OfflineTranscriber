package app.offlinetranscriber.mobile.ui.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.queue.TranscriptionJobStage
import app.offlinetranscriber.mobile.queue.TranscriptionJobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    onOpenTranscript: (transcriptId: Long, sourceType: String) -> Unit,
    viewModel: QueueViewModel = viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Transcription queue")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                actions = {
                    val hasFinished = jobs.any {
                        it.status == TranscriptionJobStatus.COMPLETED.name ||
                            it.status == TranscriptionJobStatus.CANCELLED.name
                    }
                    if (hasFinished) {
                        IconButton(onClick = viewModel::clearFinished) {
                            Icon(Icons.Default.DeleteSweep, "Clear finished")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (jobs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Queue is empty",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Imported audio or video files will appear here while they are being transcribed in the background.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    jobs,
                    key = { it.id }
                ) { job ->
                    val status = runCatching {
                        TranscriptionJobStatus.valueOf(job.status)
                    }.getOrDefault(TranscriptionJobStatus.FAILED)

                    val stage = runCatching {
                        TranscriptionJobStage.valueOf(job.stage)
                    }.getOrDefault(TranscriptionJobStage.TRANSCRIBING)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = status == TranscriptionJobStatus.COMPLETED && job.resultTranscriptId != null
                            ) {
                                job.resultTranscriptId?.let { transcriptId ->
                                    onOpenTranscript(transcriptId, job.sourceType)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when (status) {
                                TranscriptionJobStatus.PROCESSING ->
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                TranscriptionJobStatus.FAILED ->
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                else ->
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = job.sourceType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            when (status) {
                                TranscriptionJobStatus.PROCESSING -> {
                                    LinearProgressIndicator(
                                        progress = { job.progress / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    val processingLabel = if (stage == TranscriptionJobStage.PREPARING) {
                                        "Preparing video • ${job.progress}%"
                                    } else {
                                        "Transcribing • ${job.progress}%"
                                    }
                                    Text(
                                        processingLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                TranscriptionJobStatus.QUEUED -> {
                                    val queuedLabel = if (stage == TranscriptionJobStage.PREPARING) {
                                        "Video waiting to prepare"
                                    } else {
                                        "Waiting in queue"
                                    }
                                    Text(
                                        queuedLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TranscriptionJobStatus.COMPLETED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            "Completed • Tap to open",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                TranscriptionJobStatus.FAILED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text(
                                            job.errorMessage ?: "Transcription failed",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                TranscriptionJobStatus.CANCELLED -> {
                                    Text(
                                        "Cancelled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            when (status) {
                                TranscriptionJobStatus.QUEUED,
                                TranscriptionJobStatus.PROCESSING -> {
                                    OutlinedButton(
                                        onClick = { viewModel.cancel(job.id) }
                                    ) {
                                        Text("Cancel")
                                    }
                                }

                                TranscriptionJobStatus.FAILED -> {
                                    Button(
                                        onClick = { viewModel.retry(job.id) }
                                    ) {
                                        Icon(Icons.Default.Refresh, null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                }

                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}
