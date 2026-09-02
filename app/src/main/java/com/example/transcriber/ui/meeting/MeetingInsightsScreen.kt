package com.example.transcriber.ui.meeting

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.meeting.MeetingInsightsViewModel
import com.example.transcriber.meeting.MeetingNotesFormatter
import com.example.transcriber.ui.accessibility.accessibleAction
import com.example.transcriber.ui.accessibility.minimumTouchTarget
import com.example.transcriber.ui.layout.ReadingWidthContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingInsightsScreen(
    onBack: () -> Unit,
    onOpenSource: (transcriptId: Long, mediaType: String, seekMs: Long) -> Unit,
    onOpenPaywall: (ProFeature) -> Unit,
    onExport: () -> Unit = {},
    viewModel: MeetingInsightsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setForeground(true)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.setForeground(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val transcript = state.transcript
    val packState = state.pack

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Meeting Intelligence",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.semantics { heading() }
                        )
                        transcript?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumTouchTarget()
                            .accessibleAction("Back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (packState != null && !state.generating) {
                        IconButton(
                            onClick = onExport,
                            modifier = Modifier
                                .minimumTouchTarget()
                                .accessibleAction("Export and share meeting notes")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Export & Share"
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.generate(
                                    onProRequired = {
                                        onOpenPaywall(ProFeature.MEETING_INTELLIGENCE)
                                    }
                                )
                            },
                            modifier = Modifier
                                .minimumTouchTarget()
                                .accessibleAction("Regenerate meeting insights")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regenerate meeting insights"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        ReadingWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.generating -> {
                        MeetingGeneratingState(
                            progress = state.progress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    packState == null -> {
                        MeetingEmptyState(
                            onCreatePack = {
                                viewModel.generate(
                                    onProRequired = {
                                        onOpenPaywall(ProFeature.MEETING_INTELLIGENCE)
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        MeetingPackContent(
                            packState = packState,
                            selectedTab = state.selectedTab,
                            onSelectTab = { viewModel.selectTab(it) },
                            onToggleAction = { actionId, done ->
                                viewModel.toggleAction(actionId, done)
                            },
                            onEditAction = { actionId, text, assignee, dueText ->
                                viewModel.editAction(actionId, text, assignee, dueText)
                            },
                            onOpenSource = { seekMs ->
                                transcript?.let {
                                    onOpenSource(it.id, it.mediaType, seekMs)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
