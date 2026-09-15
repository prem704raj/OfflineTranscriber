package app.offlinetranscriber.mobile.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.study.StudyTab
import app.offlinetranscriber.mobile.study.StudyViewModel
import app.offlinetranscriber.mobile.study.model.FlashcardStatus
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel
import app.offlinetranscriber.mobile.ui.system.OtWaveToTextMark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onBack: () -> Unit,
    onOpenSource: (
        transcriptId: Long,
        mediaType: String,
        seekMs: Long
    ) -> Unit,
    onExport: () -> Unit = {},
    viewModel: StudyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val transcript = state.transcript
    val pack = state.pack

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Study",
                            style = MaterialTheme.typography.titleLarge
                        )
                        transcript?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.accessibleAction("Back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (pack != null) {
                        IconButton(
                            onClick = onExport,
                            modifier = Modifier.accessibleAction("Export and share")
                        ) {
                            Icon(
                                imageVector = OtIcons.Export,
                                contentDescription = "Export & Share"
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.accessibleAction("Study pack options")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Study pack options"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export & Share") },
                                    leadingIcon = {
                                        Icon(OtIcons.Export, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onExport()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Regenerate") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.generate(true)
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                DropdownMenuItem(
                                    text = { Text("Delete study pack", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.deletePack()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.generating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }

            if (pack == null) {
                StudyEmptyState(
                    nanoState = state.nanoState,
                    generating = state.generating,
                    onGenerate = { viewModel.generate(true) },
                    onGenerateClassic = { viewModel.generate(false) },
                    onDownloadNano = { viewModel.downloadNano() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val tabs = StudyTab.entries

                PrimaryTabRow(
                    selectedTabIndex = tabs.indexOf(state.activeTab),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = state.activeTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            text = {
                                Text(
                                    when (tab) {
                                        StudyTab.OVERVIEW -> "Overview"
                                        StudyTab.CHAPTERS -> "Chapters"
                                        StudyTab.FLASHCARDS -> "Cards"
                                        StudyTab.QUIZ -> "Quiz"
                                    }
                                )
                            }
                        )
                    }
                }

                when (state.activeTab) {
                    StudyTab.OVERVIEW -> OverviewTab(pack = pack)

                    StudyTab.CHAPTERS -> ChaptersTab(
                        pack = pack,
                        onOpenChapter = { startMs ->
                            val t = state.transcript ?: return@ChaptersTab
                            onOpenSource(t.id, t.mediaType, startMs)
                        }
                    )

                    StudyTab.FLASHCARDS -> FlashcardsTab(
                        state = state,
                        onFlip = viewModel::flipFlashcard,
                        onPrevious = viewModel::previousFlashcard,
                        onNext = viewModel::nextFlashcard,
                        onShuffle = viewModel::shuffleFlashcards,
                        onKnown = { viewModel.markCurrentCard(FlashcardStatus.KNOWN) },
                        onReview = { viewModel.markCurrentCard(FlashcardStatus.REVIEW) }
                    )

                    StudyTab.QUIZ -> QuizTab(
                        state = state,
                        onAnswer = viewModel::answerQuiz,
                        onNext = viewModel::nextQuizQuestion,
                        onRetry = viewModel::retryQuiz
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyEmptyState(
    nanoState: NanoFeatureState,
    generating: Boolean,
    onGenerate: () -> Unit,
    onGenerateClassic: () -> Unit,
    onDownloadNano: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReadingWidthContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OtWaveToTextMark()

            Spacer(Modifier.height(AppDimens.Space6))

            Text(
                text = "Turn this transcript into a study pack",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(AppDimens.Space2))

            Text(
                text = "Get key points, chapters, flashcards and a quiz. Everything stays on your device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(AppDimens.Space5))

            when (nanoState) {
                NanoFeatureState.Available -> {
                    OtStatusLabel(
                        text = "On-device AI ready",
                        kind = OtStatusKind.LOCAL
                    )
                }

                NanoFeatureState.Downloadable -> {
                    FilledTonalButton(
                        onClick = onDownloadNano,
                        enabled = !generating,
                        shape = AppShapes.Button
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.padding(4.dp))
                        Text("Download on-device AI")
                    }
                }

                NanoFeatureState.Downloading -> {
                    Text(
                        "Downloading on-device AI…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                else -> {
                    OtStatusLabel(
                        text = "Classic offline ready",
                        kind = OtStatusKind.LOCAL
                    )
                }
            }

            Spacer(Modifier.height(AppDimens.Space4))

            Button(
                onClick = onGenerate,
                enabled = !generating,
                shape = AppShapes.Button,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.PrimaryTouchTarget)
            ) {
                Text(
                    text = if (generating) "Generating…" else "Create study pack",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (nanoState is NanoFeatureState.Available) {
                Spacer(Modifier.height(AppDimens.Space2))

                TextButton(
                    onClick = onGenerateClassic,
                    enabled = !generating,
                    shape = AppShapes.Control
                ) {
                    Text("Use fast Classic mode")
                }
            }
        }
    }
}
