package com.example.transcriber.ui.subtitle

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.example.transcriber.TranscriberApplication
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.caption.edit.CaptionReadability
import com.example.transcriber.caption.edit.CaptionReadabilityAnalyzer
import com.example.transcriber.caption.edit.CaptionTimingIssue
import com.example.transcriber.caption.export.background.CaptionExportJobStatus
import com.example.transcriber.caption.model.CaptionCue
import com.example.transcriber.caption.model.CaptionStyle
import com.example.transcriber.caption.preview.CaptionPreviewController
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.media.MediaAvailability
import com.example.transcriber.media.MediaAvailabilityChecker
import com.example.transcriber.playback.AutoFollowController
import com.example.transcriber.subtitle.SubtitleExporter
import com.example.transcriber.subtitle.SubtitleFormat
import com.example.transcriber.subtitle.SubtitleStudioUiState
import com.example.transcriber.subtitle.SubtitleStudioViewModel
import com.example.transcriber.ui.accessibility.accessibleAction
import com.example.transcriber.ui.accessibility.minimumTouchTarget
import com.example.transcriber.ui.caption.CaptionExportProgressCard
import com.example.transcriber.ui.caption.CaptionExportResultCard
import com.example.transcriber.ui.caption.CaptionExportSheet
import com.example.transcriber.ui.caption.CaptionMorePanel
import com.example.transcriber.ui.caption.CaptionPositionPanel
import com.example.transcriber.ui.caption.CaptionPresetPanel
import com.example.transcriber.ui.caption.CaptionSpeakerPanel
import com.example.transcriber.ui.caption.CaptionStyleToolbar
import com.example.transcriber.ui.caption.CaptionTextPanel
import com.example.transcriber.ui.caption.CaptionToolPanel
import com.example.transcriber.ui.caption.SplitCaptionDialog
import com.example.transcriber.ui.layout.AppWidthClass
import com.example.transcriber.ui.layout.appWidthClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleStudioScreen(
    onBack: () -> Unit,
    onStudy: () -> Unit = {},
    onAsk: () -> Unit = {},
    onMeeting: () -> Unit = {},
    onSpeaker: () -> Unit = {},
    onExport: () -> Unit = {},
    onOpenPaywall: (ProFeature) -> Unit = {},
    viewModel: SubtitleStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = context.applicationContext as TranscriberApplication
    val entitlement by app.entitlementRepository.entitlement.collectAsState(initial = Entitlement.FREE)
    val isPro = (entitlement == Entitlement.PRO)

    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val exporter = remember(context) { SubtitleExporter(context) }
    var editing by remember { mutableStateOf<TranscriptSegment?>(null) }
    var splittingSegment by remember { mutableStateOf<TranscriptSegment?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingSaveJobId by remember { mutableStateOf<String?>(null) }

    val autoFollow = remember { AutoFollowController(4_000L) }

    // Live preview controller
    val previewController = remember(viewModel.player) {
        CaptionPreviewController(viewModel.player)
    }

    LaunchedEffect(state.cues, state.cueRevision) {
        if (state.cues.isNotEmpty()) {
            previewController.attach(state.cues, state.style)
        }
    }

    LaunchedEffect(state.style) {
        previewController.updateStyle(state.style)
    }

    DisposableEffect(previewController) {
        onDispose {
            previewController.detach()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) autoFollow.onUserScroll()
            }
    }

    val mediaChecker = remember(context) { MediaAvailabilityChecker(context) }
    val isVideoAvailable = remember(state.transcript) {
        val uri = state.transcript?.sourceUri?.ifBlank { state.transcript?.audioUriString }
        mediaChecker.check(uri) is MediaAvailability.Available
    }

    val saveVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        if (uri != null && pendingSaveJobId != null) {
            viewModel.saveExportedVideo(context, uri, pendingSaveJobId!!)
            pendingSaveJobId = null
        }
    }

    val srtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        if (uri != null) {
            runCatching { exporter.writeToUri(uri, SubtitleFormat.SRT, state.segments) }
        }
    }

    val vttLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/vtt")
    ) { uri ->
        if (uri != null) {
            runCatching { exporter.writeToUri(uri, SubtitleFormat.VTT, state.segments) }
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.clearError()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.clearMessage()
    }

    LaunchedEffect(state.activeIndex) {
        val index = state.activeIndex
        if (index >= 0 && autoFollow.shouldAutoFollow()) {
            listState.animateScrollToItem(index)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Caption Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumTouchTarget()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.padding(end = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text("Export")
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Document Hub") },
                                onClick = {
                                    menuExpanded = false
                                    onExport()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Export SRT Subtitles") },
                                onClick = {
                                    menuExpanded = false
                                    if (isPro) {
                                        srtLauncher.launch("${state.transcript?.title ?: "subtitles"}.srt")
                                    } else {
                                        onOpenPaywall(ProFeature.SUBTITLE_EXPORT)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Subtitles, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Export VTT Subtitles") },
                                onClick = {
                                    menuExpanded = false
                                    if (isPro) {
                                        vttLauncher.launch("${state.transcript?.title ?: "subtitles"}.vtt")
                                    } else {
                                        onOpenPaywall(ProFeature.SUBTITLE_EXPORT)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Subtitles, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Study Mode") },
                                onClick = {
                                    menuExpanded = false
                                    onStudy()
                                },
                                leadingIcon = { Icon(Icons.Default.School, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Ask AI") },
                                onClick = {
                                    menuExpanded = false
                                    onAsk()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Meeting Insights") },
                                onClick = {
                                    menuExpanded = false
                                    onMeeting()
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.FactCheck, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Speaker Intelligence") },
                                onClick = {
                                    menuExpanded = false
                                    onSpeaker()
                                },
                                leadingIcon = { Icon(Icons.Default.GraphicEq, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val widthClass = appWidthClass()

        if (widthClass == AppWidthClass.EXPANDED) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    PlayerPane(
                        viewModel = viewModel,
                        isVideoAvailable = isVideoAvailable,
                        modifier = Modifier.weight(1f)
                    )

                    CaptionStyleToolbar(
                        selectedPanel = state.selectedPanel,
                        hasSpeakerData = state.hasSpeakerData,
                        onSelectPanel = viewModel::setSelectedPanel
                    )

                    StylePanelHost(
                        state = state,
                        viewModel = viewModel,
                        onSpeaker = onSpeaker
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ExportJobSection(
                        job = state.exportJob,
                        viewModel = viewModel,
                        onSaveJob = { jobId ->
                            pendingSaveJobId = jobId
                            val defaultName = "${state.transcript?.title ?: "Video"} - Captioned.mp4"
                            saveVideoLauncher.launch(defaultName)
                        },
                        onShareJob = { jobId ->
                            viewModel.shareExportedVideo(context, jobId)
                        }
                    )

                    SubtitleListPane(
                        state = state,
                        listState = listState,
                        onSeekTo = viewModel::seekTo,
                        onBookmark = viewModel::toggleBookmark,
                        onEdit = { editing = it },
                        onSplit = { splittingSegment = it },
                        onMerge = { cur, next -> viewModel.mergeWithNext(cur, next) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                PlayerPane(
                    viewModel = viewModel,
                    isVideoAvailable = isVideoAvailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                CaptionStyleToolbar(
                    selectedPanel = state.selectedPanel,
                    hasSpeakerData = state.hasSpeakerData,
                    onSelectPanel = viewModel::setSelectedPanel
                )

                StylePanelHost(
                    state = state,
                    viewModel = viewModel,
                    onSpeaker = onSpeaker
                )

                ExportJobSection(
                    job = state.exportJob,
                    viewModel = viewModel,
                    onSaveJob = { jobId ->
                        pendingSaveJobId = jobId
                        val defaultName = "${state.transcript?.title ?: "Video"} - Captioned.mp4"
                        saveVideoLauncher.launch(defaultName)
                    },
                    onShareJob = { jobId ->
                        viewModel.shareExportedVideo(context, jobId)
                    }
                )

                SubtitleListPane(
                    state = state,
                    listState = listState,
                    onSeekTo = viewModel::seekTo,
                    onBookmark = viewModel::toggleBookmark,
                    onEdit = { editing = it },
                    onSplit = { splittingSegment = it },
                    onMerge = { cur, next -> viewModel.mergeWithNext(cur, next) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Edit Subtitle Dialog
    editing?.let { segment ->
        EditSubtitleDialog(
            segment = segment,
            onDismiss = { editing = null },
            onSave = { text, start, end ->
                viewModel.saveSegment(segment.id, text, start, end)
                editing = null
            }
        )
    }

    // Split Caption Dialog
    splittingSegment?.let { segment ->
        SplitCaptionDialog(
            segmentText = segment.text,
            onConfirmSplit = { splitIndex ->
                viewModel.splitSegment(segment.id, splitIndex)
                splittingSegment = null
            },
            onDismiss = { splittingSegment = null }
        )
    }

    // Export Sheet
    if (showExportSheet) {
        CaptionExportSheet(
            style = state.style,
            resolution = state.exportResolution,
            cuesCount = state.cues.size,
            durationMs = state.durationMs,
            entitlement = entitlement,
            onSelectResolution = viewModel::setExportResolution,
            onStartExport = viewModel::startExport,
            onOpenPaywall = onOpenPaywall,
            onDismiss = { showExportSheet = false }
        )
    }
}

@Composable
private fun StylePanelHost(
    state: SubtitleStudioUiState,
    viewModel: SubtitleStudioViewModel,
    onSpeaker: () -> Unit
) {
    when (state.selectedPanel) {
        CaptionToolPanel.PRESETS -> {
            CaptionPresetPanel(
                selectedPreset = state.style.preset,
                onSelectPreset = viewModel::selectPreset
            )
        }
        CaptionToolPanel.TEXT -> {
            CaptionTextPanel(
                style = state.style,
                onUpdateStyle = viewModel::setStyle
            )
        }
        CaptionToolPanel.POSITION -> {
            CaptionPositionPanel(
                style = state.style,
                onUpdateStyle = viewModel::setStyle
            )
        }
        CaptionToolPanel.SPEAKERS -> {
            CaptionSpeakerPanel(
                style = state.style,
                hasSpeakerData = state.hasSpeakerData,
                onUpdateStyle = viewModel::setStyle,
                onManageSpeakers = onSpeaker
            )
        }
        CaptionToolPanel.MORE -> {
            CaptionMorePanel(
                style = state.style,
                onUpdateStyle = viewModel::setStyle
            )
        }
        CaptionToolPanel.NONE -> Unit
    }
}

@Composable
private fun ExportJobSection(
    job: com.example.transcriber.caption.export.background.CaptionExportJobState?,
    viewModel: SubtitleStudioViewModel,
    onSaveJob: (String) -> Unit,
    onShareJob: (String) -> Unit
) {
    if (job == null) return

    when (job.status) {
        CaptionExportJobStatus.QUEUED,
        CaptionExportJobStatus.WAITING,
        CaptionExportJobStatus.EXPORTING -> {
            CaptionExportProgressCard(
                job = job,
                onCancel = viewModel::cancelExport
            )
        }
        CaptionExportJobStatus.COMPLETED -> {
            CaptionExportResultCard(
                job = job,
                onSave = onSaveJob,
                onShare = onShareJob,
                onDeleteTemp = viewModel::deleteTempExport
            )
        }
        else -> Unit
    }
}

@Composable
private fun PlayerPane(
    viewModel: SubtitleStudioViewModel,
    isVideoAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoAvailable) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Video preview unavailable",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SubtitleListPane(
    state: SubtitleStudioUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeekTo: (Long) -> Unit,
    onBookmark: (Long) -> Unit,
    onEdit: (TranscriptSegment) -> Unit,
    onSplit: (TranscriptSegment) -> Unit,
    onMerge: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cuesMap = remember(state.cues) { state.cues.associateBy { it.segmentId } }
    val issuesMap = remember(state.timingIssues) { state.timingIssues.groupBy { it.segmentId } }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = state.segments,
            key = { _, item -> item.id }
        ) { index, segment ->
            val isActive = (index == state.activeIndex)
            val cue = cuesMap[segment.id]
            val issues = issuesMap[segment.id].orEmpty()
            val nextSegment = state.segments.getOrNull(index + 1)

            CaptionSegmentRow(
                segment = segment,
                cue = cue,
                maxLines = state.style.maxLines,
                isActive = isActive,
                issues = issues,
                hasNext = nextSegment != null,
                onClick = { onSeekTo(segment.startMs) },
                onBookmark = { onBookmark(segment.id) },
                onEdit = { onEdit(segment) },
                onSplit = { onSplit(segment) },
                onMergeNext = {
                    if (nextSegment != null) {
                        onMerge(segment.id, nextSegment.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun CaptionSegmentRow(
    segment: TranscriptSegment,
    cue: CaptionCue?,
    maxLines: Int,
    isActive: Boolean,
    issues: List<CaptionTimingIssue>,
    hasNext: Boolean,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
    onEdit: () -> Unit,
    onSplit: () -> Unit,
    onMergeNext: () -> Unit
) {
    val cueForAnalysis = cue ?: CaptionCue(
        segmentId = segment.id,
        startUs = segment.startMs * 1000L,
        endUs = segment.endMs * 1000L,
        text = segment.text
    )
    val readability = remember(cueForAnalysis, maxLines) {
        CaptionReadabilityAnalyzer.analyze(cueForAnalysis, maxLines)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val startSec = segment.startMs / 1000
                    val endSec = segment.endMs / 1000
                    Text(
                        text = String.format("%d:%02d - %d:%02d", startSec / 60, startSec % 60, endSec / 60, endSec % 60),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    val speaker = cue?.speakerLabel
                    if (!speaker.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = speaker,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSplit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.CallSplit, contentDescription = "Split caption", modifier = Modifier.size(18.dp))
                    }

                    if (hasNext) {
                        IconButton(
                            onClick = onMergeNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = "Merge with next", modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit caption", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )

            // Warnings / Readability Chips
            if (issues.isNotEmpty() || readability.contains(CaptionReadability.LONG) || readability.contains(CaptionReadability.FAST)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (issue in issues) {
                        AssistChip(
                            onClick = {},
                            label = { Text(issue.message, fontSize = 10.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    if (readability.contains(CaptionReadability.LONG)) {
                        Text("• Long text", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (readability.contains(CaptionReadability.FAST)) {
                        Text("• Fast reading speed", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}
