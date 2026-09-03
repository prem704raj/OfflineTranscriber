package app.offlinetranscriber.mobile.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.offlinetranscriber.mobile.TranscriberApplication
import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.data.model.TranscriptEntity
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.media.MediaAvailability
import app.offlinetranscriber.mobile.media.MediaAvailabilityChecker
import app.offlinetranscriber.mobile.playback.AutoFollowController
import app.offlinetranscriber.mobile.playback.SegmentTimelineIndex
import app.offlinetranscriber.mobile.theme.AccentEmerald
import app.offlinetranscriber.mobile.theme.AccentRose
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget
import app.offlinetranscriber.mobile.ui.components.AudioPlayerBar
import app.offlinetranscriber.mobile.ui.components.rememberAudioPlayerState
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.GraphicEq
import app.offlinetranscriber.mobile.ui.speaker.getSpeakerColor

enum class TranscriptViewMode {
    SEGMENTS,
    FULL_TEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailScreen(
    transcript: TranscriptEntity,
    initialSeekMs: Long = 0L,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onAsk: () -> Unit = {},
    onMeeting: () -> Unit = {},
    onSpeaker: () -> Unit = {},
    onExport: () -> Unit = {},
    onCaptionStudio: (() -> Unit)? = null,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val app = remember(context) { context.applicationContext as TranscriberApplication }
    val knowledgeRepository = remember(context) { app.knowledgeRepository }
    val speakerRepository = remember(context) { app.speakerRepository }

    val bookmarkedSegmentIds by knowledgeRepository
        .observeBookmarkedSegmentIds(transcript.id)
        .collectAsState(initial = emptyList())

    val assignments by speakerRepository
        .observeAssignments(transcript.id)
        .collectAsState(initial = emptyList())

    val clusters by speakerRepository
        .observeClusters(transcript.id)
        .collectAsState(initial = emptyList())

    val assignmentMap = remember(assignments) {
        assignments.associateBy { it.segmentId }
    }

    var selectedSpeakerFilterId by remember { mutableStateOf<Long?>(null) }

    val mediaChecker = remember(context) { MediaAvailabilityChecker(context) }
    val mediaAvailability = remember(transcript.audioUriString) {
        mediaChecker.check(transcript.audioUriString)
    }
    val isMediaAvailable = mediaAvailability is MediaAvailability.Available

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(TranscriptViewMode.SEGMENTS) }
    var searchQuery by remember { mutableStateOf("") }

    val segments = remember(transcript.segmentsJson) { transcript.getSegments() }
    val filteredSegments = remember(segments, searchQuery, selectedSpeakerFilterId, assignmentMap) {
        var list = if (searchQuery.isBlank()) segments
        else segments.filter { it.text.contains(searchQuery, ignoreCase = true) }

        if (selectedSpeakerFilterId != null) {
            list = list.filter { seg ->
                assignmentMap[seg.id]?.speakerClusterId == selectedSpeakerFilterId
            }
        }
        list
    }

    val timelineIndex = remember(filteredSegments) {
        SegmentTimelineIndex.fromSegments(filteredSegments)
    }

    val audioPlayerState = rememberAudioPlayerState(if (isMediaAvailable) transcript.audioUriString else null)

    val activeIndex = remember(audioPlayerState.currentPositionMs, timelineIndex) {
        if (isMediaAvailable) {
            timelineIndex.activeIndex(audioPlayerState.currentPositionMs.toLong())
        } else {
            -1
        }
    }

    val autoFollow = remember { AutoFollowController(4_000L) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) autoFollow.onUserScroll()
            }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex !in filteredSegments.indices) return@LaunchedEffect
        if (!audioPlayerState.isPlaying) return@LaunchedEffect
        if (!autoFollow.shouldAutoFollow()) return@LaunchedEffect

        val alreadyVisible = listState.layoutInfo.visibleItemsInfo.any {
            it.index == activeIndex
        }

        if (!alreadyVisible) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -120)
        }
    }

    var seekApplied by remember { mutableStateOf(false) }
    LaunchedEffect(initialSeekMs, audioPlayerState.durationMs) {
        if (isMediaAvailable && !seekApplied && initialSeekMs > 0L && audioPlayerState.durationMs > 0L) {
            seekApplied = true
            audioPlayerState.seekTo(initialSeekMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showRenameDialog = true }
                            .minimumTouchTarget()
                    ) {
                        Text(
                            text = transcript.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumTouchTarget()
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMoreMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = onAsk,
                        modifier = Modifier.accessibleAction("Ask this transcript")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Ask this transcript",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onStudy,
                        modifier = Modifier.accessibleAction("Study this transcript")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Study this transcript",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.accessibleAction("More options")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Speaker Diarization") },
                                leadingIcon = {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onSpeaker()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Meeting Intelligence") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onMeeting()
                                }
                            )

                            if (transcript.mediaType == MediaType.VIDEO && onCaptionStudio != null) {
                                DropdownMenuItem(
                                    text = { Text("Caption Studio") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Movie, contentDescription = null)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onCaptionStudio()
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Export & Share") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onExport()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Copy Full Text") },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Transcript", transcript.fullText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Transcript copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showRenameDialog = true
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Delete", color = AccentRose) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRose)
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isMediaAvailable && !transcript.audioUriString.isNullOrBlank()) {
                AudioPlayerBar(
                    state = audioPlayerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                // Missing audio warning banner
                if (!isMediaAvailable) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The original recording was moved, deleted, or permission is no longer available. Transcript text, study mode, and search remain fully functional.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                            )
                        }
                    }
                }

                // Metadata Header
                val dateFormatted = remember(transcript.createdAt) {
                    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
                    sdf.format(Date(transcript.createdAt))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredSegments.size} segments • ${TranscriptSegment.formatTime(transcript.audioDurationMs)}",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Search bar & Mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transcript...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        FilterChip(
                            selected = viewMode == TranscriptViewMode.SEGMENTS,
                            onClick = { viewMode = TranscriptViewMode.SEGMENTS },
                            label = { Icon(imageVector = Icons.AutoMirrored.Filled.ViewList, contentDescription = "Segments", modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.minimumTouchTarget()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = viewMode == TranscriptViewMode.FULL_TEXT,
                            onClick = { viewMode = TranscriptViewMode.FULL_TEXT },
                            label = { Icon(imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Full Text", modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.minimumTouchTarget()
                        )
                    }
                }

                // Speaker Filter Chips Row
                if (clusters.isNotEmpty() && viewMode == TranscriptViewMode.SEGMENTS) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSpeakerFilterId == null,
                                onClick = { selectedSpeakerFilterId = null },
                                label = { Text("All (${clusters.size} speakers)") }
                            )
                        }
                        items(clusters.size) { idx ->
                            val cluster = clusters[idx]
                            val color = getSpeakerColor(cluster.speakerIndex)
                            FilterChip(
                                selected = selectedSpeakerFilterId == cluster.id,
                                onClick = {
                                    selectedSpeakerFilterId = if (selectedSpeakerFilterId == cluster.id) null else cluster.id
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(color)
                                    )
                                },
                                label = { Text(cluster.customName) }
                            )
                        }
                    }
                }

                // Resume auto-follow button if user scrolled away
                if (isMediaAvailable && audioPlayerState.isPlaying && !autoFollow.shouldAutoFollow() && activeIndex in filteredSegments.indices) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalButton(
                            onClick = {
                                autoFollow.forceResume()
                                coroutineScope.launch {
                                    listState.animateScrollToItem(activeIndex, scrollOffset = -120)
                                }
                            },
                            modifier = Modifier.minimumTouchTarget()
                        ) {
                            Text("Jump to current moment")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Transcript Body
                when (viewMode) {
                    TranscriptViewMode.FULL_TEXT -> {
                        SelectionContainer {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = transcript.fullText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }

                    TranscriptViewMode.SEGMENTS -> {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredSegments, key = { _, it -> it.id }) { index, segment ->
                                val isBookmarked = segment.id in bookmarkedSegmentIds
                                val isPlayingNow = isMediaAvailable && audioPlayerState.isPlaying && index == activeIndex
                                val currentAssignment = assignmentMap[segment.id]
                                val prevAssignment = if (index > 0) assignmentMap[filteredSegments[index - 1].id] else null
                                val isSpeakerTransition = currentAssignment != null && (prevAssignment?.speakerClusterId != currentAssignment.speakerClusterId)

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (isSpeakerTransition && currentAssignment != null) {
                                        val color = getSpeakerColor(currentAssignment.speakerIndex)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = currentAssignment.customName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    SegmentCard(
                                        segment = segment,
                                        isBookmarked = isBookmarked,
                                        isPlayingNow = isPlayingNow,
                                        speakerAssignment = currentAssignment,
                                        onClick = {
                                            if (isMediaAvailable) {
                                                audioPlayerState.seekTo(segment.startMs)
                                                if (!audioPlayerState.isPlaying) {
                                                    audioPlayerState.togglePlay()
                                                }
                                            }
                                        },
                                        onToggleBookmark = {
                                            coroutineScope.launch {
                                                knowledgeRepository.toggleBookmark(
                                                    transcriptId = transcript.id,
                                                    segmentId = segment.id
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(transcript.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Transcript", modifier = Modifier.semantics { heading() }) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRename(newTitle.trim())
                            showRenameDialog = false
                        }
                    },
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false },
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transcript", modifier = Modifier.semantics { heading() }) },
            text = { Text("Are you sure you want to permanently delete this transcript?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Text("Delete", color = AccentRose)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SegmentCard(
    segment: TranscriptSegment,
    isBookmarked: Boolean,
    isPlayingNow: Boolean,
    speakerAssignment: app.offlinetranscriber.mobile.data.database.SpeakerAssignmentRow? = null,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingNow) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPlayingNow) Icons.Default.PlayArrow else Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = segment.formattedTimestamp,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    if (speakerAssignment != null) {
                        val color = getSpeakerColor(speakerAssignment.speakerIndex)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = speakerAssignment.customName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = color
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .size(36.dp)
                        .accessibleAction(if (isBookmarked) "Remove bookmark" else "Bookmark moment")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = if (isPlayingNow) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
