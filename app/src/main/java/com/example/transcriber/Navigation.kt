package com.example.transcriber

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.transcriber.billing.Entitlement
import com.example.transcriber.billing.ProFeature
import com.example.transcriber.data.model.MediaType
import com.example.transcriber.shortcuts.ExternalAppCommand
import com.example.transcriber.ui.about.AboutScreen
import com.example.transcriber.ui.accessibility.minimumTouchTarget
import com.example.transcriber.ui.detail.TranscriptDetailScreen
import com.example.transcriber.ui.diagnostics.DiagnosticsScreen
import com.example.transcriber.ui.home.HomeScreen
import com.example.transcriber.ui.home.HomeViewModel
import com.example.transcriber.ui.layout.AppWidthClass
import com.example.transcriber.ui.layout.appWidthClass
import com.example.transcriber.ui.library.CollectionDetailScreen
import com.example.transcriber.ui.library.LibraryScreen
import com.example.transcriber.ui.modelmanager.ModelManagerScreen
import com.example.transcriber.ui.onboarding.OnboardingScreen
import com.example.transcriber.ui.paywall.PaywallScreen
import com.example.transcriber.ui.privacy.PrivacyDataScreen
import com.example.transcriber.ui.queue.QueueScreen
import com.example.transcriber.ui.search.SearchScreen
import com.example.transcriber.ui.settings.SettingsScreen
import com.example.transcriber.ui.study.StudyScreen
import com.example.transcriber.ui.subtitle.SubtitleStudioScreen
import com.example.transcriber.ui.video.VideoImportScreen
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as TranscriberApplication
    val scope = rememberCoroutineScope()

    val settings by app.settingsRepository.settings.collectAsState(initial = null)
    val entitlement by app.entitlementRepository.entitlement.collectAsState(initial = Entitlement.FREE)

    val backStack = rememberNavBackStack(HomeNavKey)
    val homeViewModel: HomeViewModel = viewModel()
    val transcripts by homeViewModel.transcripts.collectAsState()

    // Handle launcher shortcuts & external commands
    LaunchedEffect(Unit) {
        app.shortcutCommandRouter.commands.collect { command ->
            when (command) {
                ExternalAppCommand.Record -> {
                    if (backStack.lastOrNull() !is HomeNavKey) {
                        backStack.clear()
                        backStack.add(HomeNavKey)
                    }
                }
                ExternalAppCommand.ImportAudio -> {
                    if (backStack.lastOrNull() !is HomeNavKey) {
                        backStack.clear()
                        backStack.add(HomeNavKey)
                    }
                }
                ExternalAppCommand.OpenQueue -> {
                    backStack.add(QueueNavKey)
                }
                ExternalAppCommand.OpenModels -> {
                    backStack.add(ModelManagerNavKey)
                }
                is ExternalAppCommand.OpenPaywall -> {
                    backStack.add(PaywallNavKey(command.feature?.name))
                }
            }
        }
    }

    // Handle initial onboarding redirection
    LaunchedEffect(settings) {
        val currentSettings = settings ?: return@LaunchedEffect
        if (!currentSettings.onboardingComplete) {
            if (backStack.lastOrNull() !is OnboardingNavKey && backStack.lastOrNull() !is ModelManagerNavKey) {
                backStack.clear()
                backStack.add(OnboardingNavKey)
            }
        }
    }

    val currentKey = backStack.lastOrNull()
    val isTopLevel = currentKey is HomeNavKey || currentKey is SearchNavKey || currentKey is LibraryNavKey
    val widthClass = appWidthClass()

    val navDisplayContent: @Composable (Modifier) -> Unit = { navModifier ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = navModifier,
            entryProvider = entryProvider {
                entry<OnboardingNavKey> {
                    val recommended = remember { app.deviceModelAdvisor.recommend() }
                    OnboardingScreen(
                        recommendedLabel = recommended.label,
                        onFinish = {
                            scope.launch {
                                app.settingsRepository.setOnboardingComplete(true)
                                backStack.clear()
                                backStack.add(HomeNavKey)
                            }
                        },
                        onOpenModels = {
                            backStack.add(ModelManagerNavKey)
                        }
                    )
                }

                entry<HomeNavKey> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onTranscriptClick = { transcript ->
                            if (transcript.mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(transcript.id, 0L))
                            } else {
                                backStack.add(DetailNavKey(transcript.id, 0L))
                            }
                        },
                        onNavigateToVideoPrepare = { uri ->
                            backStack.add(VideoPrepareNavKey(uri.toString()))
                        },
                        onOpenQueue = {
                            backStack.add(QueueNavKey)
                        },
                        onOpenModels = {
                            backStack.add(ModelManagerNavKey)
                        },
                        onOpenSettings = {
                            backStack.add(SettingsNavKey)
                        },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        }
                    )
                }

                entry<QueueNavKey> {
                    QueueScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenTranscript = { transcriptId, sourceType ->
                            if (sourceType == "VIDEO") {
                                backStack.add(SubtitleNavKey(transcriptId, 0L))
                            } else {
                                backStack.add(DetailNavKey(transcriptId, 0L))
                            }
                        }
                    )
                }

                entry<SettingsNavKey> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onModels = { backStack.add(ModelManagerNavKey) },
                        onQueue = { backStack.add(QueueNavKey) },
                        onPaywall = { backStack.add(PaywallNavKey()) },
                        onPrivacyData = { backStack.add(PrivacyDataNavKey) },
                        onDiagnostics = { backStack.add(DiagnosticsNavKey) },
                        onAbout = { backStack.add(AboutNavKey) },
                        onBackupRestore = { backStack.add(BackupRestoreNavKey) }
                    )
                }

                entry<PrivacyDataNavKey> {
                    PrivacyDataScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onDiagnostics = { backStack.add(DiagnosticsNavKey) }
                    )
                }

                entry<DiagnosticsNavKey> {
                    DiagnosticsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<AboutNavKey> {
                    AboutScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<ModelManagerNavKey> {
                    ModelManagerScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        }
                    )
                }

                entry<PaywallNavKey> { key ->
                    val reason = key.reason?.let {
                        runCatching { ProFeature.valueOf(it) }.getOrNull()
                    }
                    PaywallScreen(
                        reason = reason,
                        onBack = { backStack.removeLastOrNull() },
                        onUnlocked = { backStack.removeLastOrNull() }
                    )
                }

                entry<SearchNavKey> {
                    SearchScreen(
                        onOpenResult = { result ->
                            if (result.mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(result.transcriptId, result.startMs))
                            } else {
                                backStack.add(DetailNavKey(result.transcriptId, result.startMs))
                            }
                        }
                    )
                }

                entry<LibraryNavKey> {
                    LibraryScreen(
                        onOpenCollection = { collectionId ->
                            backStack.add(CollectionDetailNavKey(collectionId))
                        },
                        onOpenTranscript = { id, mediaType, seekMs ->
                            if (mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(id, seekMs ?: 0L))
                            } else {
                                backStack.add(DetailNavKey(id, seekMs ?: 0L))
                            }
                        },
                        onAskLibrary = {
                            if (entitlement == Entitlement.PRO) {
                                backStack.add(AskNavKey("LIBRARY", null))
                            } else {
                                backStack.add(PaywallNavKey(ProFeature.ASK_TRANSCRIPTS.name))
                            }
                        },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        }
                    )
                }

                entry<CollectionDetailNavKey> { key ->
                    CollectionDetailScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenTranscript = { id, mediaType ->
                            if (mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(id, 0L))
                            } else {
                                backStack.add(DetailNavKey(id, 0L))
                            }
                        }
                    )
                }

                entry<DetailNavKey> { key ->
                    val transcript = transcripts.firstOrNull { it.id == key.transcriptId }
                    if (transcript != null) {
                        TranscriptDetailScreen(
                            transcript = transcript,
                            initialSeekMs = key.seekMs,
                            onBack = { backStack.removeLastOrNull() },
                            onStudy = {
                                if (entitlement == Entitlement.PRO) {
                                    backStack.add(StudyNavKey(transcript.id))
                                } else {
                                    backStack.add(PaywallNavKey(ProFeature.STUDY_MODE.name))
                                }
                            },
                            onAsk = {
                                if (entitlement == Entitlement.PRO) {
                                    backStack.add(AskNavKey("TRANSCRIPT", transcript.id))
                                } else {
                                    backStack.add(PaywallNavKey(ProFeature.ASK_TRANSCRIPTS.name))
                                }
                            },
                            onMeeting = {
                                if (entitlement == Entitlement.PRO) {
                                    backStack.add(MeetingNavKey(transcript.id))
                                } else {
                                    backStack.add(PaywallNavKey(ProFeature.MEETING_INTELLIGENCE.name))
                                }
                            },
                            onSpeaker = {
                                if (entitlement == Entitlement.PRO) {
                                    backStack.add(SpeakerNavKey(transcript.id))
                                } else {
                                    backStack.add(PaywallNavKey(ProFeature.SPEAKER_INTELLIGENCE.name))
                                }
                            },
                            onExport = {
                                backStack.add(ExportNavKey("TRANSCRIPT", transcript.id))
                            },
                            onRename = { newTitle ->
                                homeViewModel.renameTranscript(transcript.id, newTitle)
                            },
                            onDelete = {
                                homeViewModel.deleteTranscript(transcript.id)
                                backStack.removeLastOrNull()
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                entry<VideoPrepareNavKey> { key ->
                    VideoImportScreen(
                        selectedVideoUri = Uri.parse(key.videoUriString),
                        onPrepared = { sourceVideoUri, extractedAudioUri, displayName ->
                            homeViewModel.enqueueVideo(
                                sourceVideoUri = sourceVideoUri,
                                extractedAudioUri = extractedAudioUri,
                                displayName = displayName
                            ) {
                                backStack.removeLastOrNull()
                                backStack.add(QueueNavKey)
                            }
                        },
                        onCancel = { backStack.removeLastOrNull() }
                    )
                }

                entry<SubtitleNavKey> { key ->
                    SubtitleStudioScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onStudy = {
                            if (entitlement == Entitlement.PRO) {
                                backStack.add(StudyNavKey(key.transcriptId))
                            } else {
                                backStack.add(PaywallNavKey(ProFeature.STUDY_MODE.name))
                            }
                        },
                        onAsk = {
                            if (entitlement == Entitlement.PRO) {
                                backStack.add(AskNavKey("TRANSCRIPT", key.transcriptId))
                            } else {
                                backStack.add(PaywallNavKey(ProFeature.ASK_TRANSCRIPTS.name))
                            }
                        },
                        onMeeting = {
                            if (entitlement == Entitlement.PRO) {
                                backStack.add(MeetingNavKey(key.transcriptId))
                            } else {
                                backStack.add(PaywallNavKey(ProFeature.MEETING_INTELLIGENCE.name))
                            }
                        },
                        onSpeaker = {
                            if (entitlement == Entitlement.PRO) {
                                backStack.add(SpeakerNavKey(key.transcriptId))
                            } else {
                                backStack.add(PaywallNavKey(ProFeature.SPEAKER_INTELLIGENCE.name))
                            }
                        },
                        onExport = {
                            backStack.add(ExportNavKey("TRANSCRIPT", key.transcriptId))
                        },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        }
                    )
                }

                entry<StudyNavKey> { key ->
                    StudyScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenSource = { id, mediaType, seekMs ->
                            if (mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(id, seekMs))
                            } else {
                                backStack.add(DetailNavKey(id, seekMs))
                            }
                        },
                        onExport = {
                            backStack.add(ExportNavKey("STUDY_PACK", key.transcriptId))
                        }
                    )
                }

                entry<AskNavKey> {
                    com.example.transcriber.ui.ask.AskScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenSource = { id, mediaType, seekMs ->
                            if (mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(id, seekMs))
                            } else {
                                backStack.add(DetailNavKey(id, seekMs))
                            }
                        },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        },
                        onExport = { convId ->
                            backStack.add(ExportNavKey("ASK_CONVERSATION", convId))
                        }
                    )
                }

                entry<MeetingNavKey> { key ->
                    com.example.transcriber.ui.meeting.MeetingInsightsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenSource = { id, mediaType, seekMs ->
                            if (mediaType == MediaType.VIDEO) {
                                backStack.add(SubtitleNavKey(id, seekMs))
                            } else {
                                backStack.add(DetailNavKey(id, seekMs))
                            }
                        },
                        onOpenPaywall = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        },
                        onExport = {
                            backStack.add(ExportNavKey("MEETING_PACK", key.transcriptId))
                        }
                    )
                }

                entry<SpeakerNavKey> { key ->
                    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as TranscriberApplication
                    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                        com.example.transcriber.ui.speaker.SpeakerViewModel(app, key.transcriptId)
                    }
                    com.example.transcriber.ui.speaker.SpeakerScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenPaywall = {
                            backStack.add(PaywallNavKey(ProFeature.SPEAKER_INTELLIGENCE.name))
                        }
                    )
                }

                entry<ExportNavKey> { key ->
                    com.example.transcriber.ui.export.ExportHubScreen(
                        contentType = key.contentType,
                        sourceId = key.sourceId,
                        onBack = { backStack.removeLastOrNull() },
                        onUpgrade = { feature ->
                            backStack.add(PaywallNavKey(feature.name))
                        }
                    )
                }

                entry<BackupRestoreNavKey> {
                    val viewModel: com.example.transcriber.ui.backup.BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    com.example.transcriber.ui.backup.BackupRestoreScreen(
                        viewModel = viewModel,
                        onCreateBackupClick = { backStack.add(CreateBackupNavKey) },
                        onNavigateToPreview = { backStack.add(RestorePreviewNavKey) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<CreateBackupNavKey> {
                    val viewModel: com.example.transcriber.ui.backup.BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    com.example.transcriber.ui.backup.CreateBackupScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<RestorePreviewNavKey> {
                    val viewModel: com.example.transcriber.ui.backup.BackupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val inspection by viewModel.lastInspection.collectAsState()
                    val preview = inspection
                    if (preview != null) {
                        com.example.transcriber.ui.backup.RestorePreviewScreen(
                            viewModel = viewModel,
                            preview = preview,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    } else {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            backStack.removeLastOrNull()
                        }
                    }
                }
            }
        )
    }

    if (isTopLevel && widthClass != AppWidthClass.COMPACT) {
        // Tablet / Foldable (>= 600dp): NavigationRail on the left
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                NavigationRailItem(
                    selected = currentKey is HomeNavKey,
                    onClick = {
                        if (currentKey !is HomeNavKey) {
                            backStack.clear()
                            backStack.add(HomeNavKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.minimumTouchTarget()
                )

                NavigationRailItem(
                    selected = currentKey is SearchNavKey,
                    onClick = {
                        if (currentKey !is SearchNavKey) {
                            backStack.clear()
                            backStack.add(SearchNavKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    modifier = Modifier.minimumTouchTarget()
                )

                NavigationRailItem(
                    selected = currentKey is LibraryNavKey,
                    onClick = {
                        if (currentKey !is LibraryNavKey) {
                            backStack.clear()
                            backStack.add(LibraryNavKey)
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library") },
                    label = { Text("Library") },
                    modifier = Modifier.minimumTouchTarget()
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                navDisplayContent(Modifier.fillMaxSize())
            }
        }
    } else {
        // Compact Screen (< 600dp) or Non-top-level
        Scaffold(
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentKey is HomeNavKey,
                            onClick = {
                                if (currentKey !is HomeNavKey) {
                                    backStack.clear()
                                    backStack.add(HomeNavKey)
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = currentKey is SearchNavKey,
                            onClick = {
                                if (currentKey !is SearchNavKey) {
                                    backStack.clear()
                                    backStack.add(SearchNavKey)
                                }
                            },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search") }
                        )

                        NavigationBarItem(
                            selected = currentKey is LibraryNavKey,
                            onClick = {
                                if (currentKey !is LibraryNavKey) {
                                    backStack.clear()
                                    backStack.add(LibraryNavKey)
                                }
                            },
                            icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Library") },
                            label = { Text("Library") }
                        )
                    }
                }
            }
        ) { rootPadding ->
            navDisplayContent(
                Modifier.padding(if (isTopLevel) rootPadding else PaddingValues(0.dp))
            )
        }
    }
}
