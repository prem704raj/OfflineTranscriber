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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.queue.TranscriptionJobStage
import app.offlinetranscriber.mobile.queue.TranscriptionJobStatus
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtEmptyState
import app.offlinetranscriber.mobile.ui.system.OtOperationStrip
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel

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
                    Text(
                        "Transcription queue",
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
                actions = {
                    val hasFinished = jobs.any {
                        it.status == TranscriptionJobStatus.COMPLETED.name ||
                            it.status == TranscriptionJobStatus.CANCELLED.name
                    }
                    if (hasFinished) {
                        IconButton(
                            onClick = viewModel::clearFinished,
                            modifier = Modifier.accessibleAction("Clear finished jobs")
                        ) {
                            Icon(Icons.Default.DeleteSweep, "Clear finished")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (jobs.isEmpty()) {
                OtEmptyState(
                    title = "Queue is empty",
                    body = "Imported audio or video files will appear here while they are being transcribed in the background.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.Space8)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = AppDimens.ScreenHorizontal,
                        vertical = AppDimens.Space4
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
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

                        val isClickable = status == TranscriptionJobStatus.COMPLETED && job.resultTranscriptId != null

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = isClickable,
                                    role = Role.Button
                                ) {
                                    job.resultTranscriptId?.let { transcriptId ->
                                        onOpenTranscript(transcriptId, job.sourceType)
                                    }
                                }
                                .padding(vertical = AppDimens.Space2)
                        ) {
                            if (status == TranscriptionJobStatus.PROCESSING) {
                                val processingLabel = if (stage == TranscriptionJobStage.PREPARING) {
                                    "Preparing video"
                                } else {
                                    "Transcribing"
                                }
                                OtOperationStrip(
                                    title = job.displayName,
                                    progress = job.progress,
                                    secondary = "$processingLabel • ${job.sourceType}",
                                    onClick = null
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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

                                Spacer(Modifier.height(AppDimens.Space1))

                                when (status) {
                                    TranscriptionJobStatus.QUEUED -> {
                                        val queuedLabel = if (stage == TranscriptionJobStage.PREPARING) {
                                            "Video waiting to prepare"
                                        } else {
                                            "Waiting in queue"
                                        }
                                        OtStatusLabel(
                                            text = queuedLabel,
                                            kind = OtStatusKind.PROCESSING
                                        )
                                    }

                                    TranscriptionJobStatus.COMPLETED -> {
                                        OtStatusLabel(
                                            text = "Completed • Tap to open",
                                            kind = OtStatusKind.LOCAL
                                        )
                                    }

                                    TranscriptionJobStatus.FAILED -> {
                                        Text(
                                            text = job.errorMessage ?: "Transcription failed",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    TranscriptionJobStatus.CANCELLED -> {
                                        Text(
                                            text = "Cancelled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    else -> Unit
                                }
                            }

                            when (status) {
                                TranscriptionJobStatus.QUEUED,
                                TranscriptionJobStatus.PROCESSING -> {
                                    Spacer(Modifier.height(AppDimens.Space2))
                                    OutlinedButton(
                                        onClick = { viewModel.cancel(job.id) },
                                        shape = AppShapes.Control
                                    ) {
                                        Text("Cancel")
                                    }
                                }

                                TranscriptionJobStatus.FAILED -> {
                                    Spacer(Modifier.height(AppDimens.Space2))
                                    Button(
                                        onClick = { viewModel.retry(job.id) },
                                        shape = AppShapes.Control
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Retry")
                                    }
                                }

                                else -> Unit
                            }

                            Spacer(Modifier.height(AppDimens.Space2))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}
