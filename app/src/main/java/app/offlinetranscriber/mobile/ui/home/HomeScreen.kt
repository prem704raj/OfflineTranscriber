package app.offlinetranscriber.mobile.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.offlinetranscriber.mobile.billing.Entitlement
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.queue.TranscriptionJobStatus
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.components.ProFeatureBadge
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtArchiveRow
import app.offlinetranscriber.mobile.ui.system.OtEmptyState
import app.offlinetranscriber.mobile.ui.system.OtOperationStrip
import app.offlinetranscriber.mobile.ui.system.OtSectionHeader
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel
import app.offlinetranscriber.mobile.ui.system.OtWaveToTextMark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTranscriptClick: (TranscriptEntity) -> Unit,
    onNavigateToVideoPrepare: (Uri) -> Unit,
    onNavigateToRecord: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPaywall: (ProFeature) -> Unit
) {
    val context = LocalContext.current
    val transcripts by viewModel.transcripts.collectAsState()
    val queueJobs by viewModel.queueJobs.collectAsState()
    val selectedModelSpec by viewModel.selectedModelSpec.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()
    val message by viewModel.message.collectAsState()
    val openPaywallFeature by viewModel.openPaywallFeature.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(openPaywallFeature) {
        openPaywallFeature?.let { feature ->
            onOpenPaywall(feature)
            viewModel.clearPaywallTrigger()
        }
    }

    val isPro = (entitlement == Entitlement.PRO)

    val activeJobs = queueJobs.filter {
        it.status == TranscriptionJobStatus.PROCESSING.name ||
            it.status == TranscriptionJobStatus.QUEUED.name
    }
    val processingJob = queueJobs.firstOrNull {
        it.status == TranscriptionJobStatus.PROCESSING.name
    }
    val queuedCount = queueJobs.count {
        it.status == TranscriptionJobStatus.QUEUED.name
    }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.enqueueAudio(it)
        }
    }

    // Video file picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onNavigateToVideoPrepare(uri)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Offline Transcriber",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isPro) {
                            ProFeatureBadge()
                        } else {
                            OtStatusLabel(
                                text = "OFFLINE",
                                kind = OtStatusKind.LOCAL
                            )
                        }
                    }
                },
                actions = {
                    // Queue Button with Badge
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.accessibleAction("Transcription queue")
                    ) {
                        if (activeJobs.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text("${activeJobs.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Queue,
                                    contentDescription = null
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Queue,
                                contentDescription = null
                            )
                        }
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.accessibleAction("Settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = AppDimens.ScreenHorizontal,
                    vertical = AppDimens.Space4
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space5)
            ) {
                // Active Processing Queue Strip (if jobs are running or queued)
                if (activeJobs.isNotEmpty()) {
                    item {
                        val titleText = if (processingJob != null) {
                            "Transcribing: ${processingJob.displayName}"
                        } else {
                            "Transcription queue active"
                        }
                        val secondaryText = if (queuedCount > 0) {
                            "$queuedCount more waiting in queue"
                        } else null

                        OtOperationStrip(
                            title = titleText,
                            progress = processingJob?.progress,
                            secondary = secondaryText,
                            onClick = onOpenQueue
                        )
                    }
                }

                // Model status row
                item {
                    ModelStatusRow(
                        label = selectedModelSpec?.label ?: "Base (Balanced)",
                        onClick = onOpenModels
                    )
                }

                // Primary Action Area: Capture Deck
                item {
                    CaptureDeck(
                        isPro = isPro,
                        onRecord = onNavigateToRecord,
                        onImportAudio = {
                            audioPickerLauncher.launch(
                                arrayOf("audio/*", "application/ogg")
                            )
                        },
                        onImportVideo = {
                            if (isPro) {
                                videoPickerLauncher.launch(arrayOf("video/*"))
                            } else {
                                onOpenPaywall(ProFeature.VIDEO_TRANSCRIPTION)
                            }
                        }
                    )
                }

                // Recent Transcripts Header
                item {
                    OtSectionHeader(
                        title = "Recent Transcripts",
                        trailing = {
                            Text(
                                text = "${transcripts.size} items",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // Transcripts List or Empty State
                if (transcripts.isEmpty()) {
                    item {
                        OtEmptyState(
                            title = "No transcripts yet",
                            body = "Record audio or import an audio/video file to create your first offline transcript or subtitles.",
                            modifier = Modifier.padding(vertical = AppDimens.Space6)
                        )
                    }
                } else {
                    items(transcripts, key = { it.id }) { transcript ->
                        val isVideo = transcript.mediaType == MediaType.VIDEO
                        val icon = if (isVideo) OtIcons.VideoImport else OtIcons.Transcript
                        val dateFormatted = SimpleDateFormat("MMM dd", Locale.getDefault())
                            .format(Date(transcript.createdAt))
                        val durationFormatted = TranscriptSegment.formatTime(transcript.audioDurationMs)

                        OtArchiveRow(
                            icon = icon,
                            title = transcript.title,
                            snippet = transcript.fullText.ifBlank { null },
                            primaryMeta = durationFormatted,
                            secondaryMeta = if (isVideo) "VIDEO • $dateFormatted" else dateFormatted,
                            onClick = { onTranscriptClick(transcript) },
                            trailing = {
                                IconButton(
                                    onClick = { viewModel.deleteTranscript(transcript.id) },
                                    modifier = Modifier.accessibleAction("Delete transcript ${transcript.title}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureDeck(
    isPro: Boolean,
    onRecord: () -> Unit,
    onImportAudio: () -> Unit,
    onImportVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.Space2)
    ) {
        Text(
            text = "Transcribe & Generate Subtitles",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(AppDimens.Space2))

        Text(
            text = "Transcribe audio or extract audio from videos and edit synchronized subtitles completely offline.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(AppDimens.Space5))

        OtWaveToTextMark(
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(AppDimens.Space4))

        Button(
            onClick = onRecord,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppDimens.PrimaryTouchTarget),
            shape = AppShapes.Button
        ) {
            Icon(
                imageVector = OtIcons.RecordWave,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Record Audio",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(AppDimens.Space2))

        OutlinedButton(
            onClick = onImportAudio,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = AppShapes.Button
        ) {
            Icon(
                imageVector = OtIcons.AudioImport,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Import Audio File",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(AppDimens.Space2))

        OutlinedButton(
            onClick = onImportVideo,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = AppShapes.Button
        ) {
            Icon(
                imageVector = OtIcons.VideoImport,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Import Video (Subtitle Studio)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!isPro) {
                Spacer(Modifier.width(8.dp))
                ProFeatureBadge()
            }
        }
    }
}

@Composable
private fun ModelStatusRow(
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.Space3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = OtIcons.Transcript,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )

            Spacer(Modifier.width(AppDimens.Space3))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Model: $label",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Tap to switch or manage Whisper models",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
