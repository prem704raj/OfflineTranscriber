package app.offlinetranscriber.mobile.ui.speaker

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.speaker.model.SpeakerDiarizationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerScreen(
    viewModel: SpeakerViewModel,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onSeekToTimestampMs: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Speaker Intelligence") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.run?.status == SpeakerDiarizationStatus.COMPLETED.name) {
                        IconButton(onClick = { viewModel.onAnalyzeClicked() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Re-analyze speakers")
                        }
                        IconButton(onClick = { viewModel.deleteDiarization() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Clear speaker data")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Diarization in progress
                state.run?.status in listOf(
                    SpeakerDiarizationStatus.PREPARING_AUDIO.name,
                    SpeakerDiarizationStatus.DIARIZING.name,
                    SpeakerDiarizationStatus.ALIGNING.name
                ) -> {
                    DiarizationProgressView(state = state)
                }

                // Diarization completed with clusters
                state.run?.status == SpeakerDiarizationStatus.COMPLETED.name && state.clusters.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Speakers (${state.clusters.size})") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Timeline (${state.turns.size})") }
                            )
                        }

                        when (selectedTab) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.clusterStats, key = { it.cluster.id }) { stats ->
                                        SpeakerRow(
                                            stats = stats,
                                            isSelected = (state.selectedSpeakerFilterId == stats.cluster.id),
                                            showMergeOption = state.clusters.size > 1,
                                            onFilterToggle = { viewModel.selectSpeakerFilter(stats.cluster.id) },
                                            onRenameClicked = { viewModel.openRenameDialog(stats.cluster) },
                                            onMergeClicked = { viewModel.openMergeDialog(stats.cluster) }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                SpeakerTimeline(
                                    turns = state.turns,
                                    onSeekToMs = onSeekToTimestampMs,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                // Empty / Not Started
                else -> {
                    SpeakerEmptyState(
                        isPro = state.isPro,
                        onAnalyzeClicked = {
                            if (!state.isPro) onOpenPaywall()
                            else viewModel.onAnalyzeClicked()
                        }
                    )
                }
            }

            // Dialogs
            if (state.showCountDialog) {
                SpeakerCountDialog(
                    onDismiss = viewModel::dismissCountDialog,
                    onConfirm = viewModel::startDiarization
                )
            }

            if (state.showSetupDialog) {
                SpeakerModelSetupDialog(
                    downloadState = state.downloadState,
                    onDownloadClicked = viewModel::downloadModels,
                    onDismiss = viewModel::dismissSetupDialog
                )
            }

            state.showRenameDialog?.let { cluster ->
                RenameSpeakerDialog(
                    cluster = cluster,
                    onDismiss = viewModel::dismissRenameDialog,
                    onConfirm = viewModel::confirmRename
                )
            }

            state.showMergeDialog?.let { cluster ->
                MergeSpeakerDialog(
                    sourceCluster = cluster,
                    allClusters = state.clusters,
                    onDismiss = viewModel::dismissMergeDialog,
                    onConfirm = viewModel::confirmMerge
                )
            }
        }
    }
}

@Composable
private fun DiarizationProgressView(state: SpeakerScreenUiState) {
    val progress = state.run?.progress ?: 0
    val status = state.run?.status

    val label = when (status) {
        SpeakerDiarizationStatus.PREPARING_AUDIO.name -> "Preparing high-quality 16kHz audio..."
        SpeakerDiarizationStatus.DIARIZING.name -> "Separating speaker voice patterns..."
        SpeakerDiarizationStatus.ALIGNING.name -> "Aligning speaker labels to transcript..."
        else -> "Processing..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { (progress / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "$progress%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SpeakerEmptyState(
    isPro: Boolean,
    onAnalyzeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Offline Speaker Diarization",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Detect who spoke when, rename speakers, and view speaker-aware transcript blocks with 100% on-device privacy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onAnalyzeClicked,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            if (!isPro) {
                Icon(androidx.compose.material.icons.Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Pro")
            } else {
                Icon(androidx.compose.material.icons.Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Analyze Speakers")
            }
        }
    }
}
