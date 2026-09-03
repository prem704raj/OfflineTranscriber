package app.offlinetranscriber.mobile.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.study.StudyTab
import app.offlinetranscriber.mobile.study.StudyViewModel
import app.offlinetranscriber.mobile.study.model.FlashcardStatus
import app.offlinetranscriber.mobile.study.nano.NanoFeatureState

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
                        Text("Study")
                        transcript?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (pack != null) {
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Share,
                                contentDescription = "Export & Share"
                            )
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    menuExpanded = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Study pack options"
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = {
                                    menuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Export & Share")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.Share,
                                            null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onExport()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Regenerate")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Refresh,
                                            null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.generate(true)
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Delete study pack")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            null
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
                }
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (pack == null) {
                StudyEmptyState(
                    nanoState = state.nanoState,
                    generating = state.generating,
                    onGenerate = {
                        viewModel.generate(true)
                    },
                    onGenerateClassic = {
                        viewModel.generate(false)
                    },
                    onDownloadNano = {
                        viewModel.downloadNano()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val tabs = StudyTab.entries

                PrimaryTabRow(
                    selectedTabIndex = tabs.indexOf(state.activeTab)
                ) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = state.activeTab == tab,
                            onClick = {
                                viewModel.setTab(tab)
                            },
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
                    StudyTab.OVERVIEW ->
                        OverviewTab(
                            pack = pack
                        )

                    StudyTab.CHAPTERS ->
                        ChaptersTab(
                            pack = pack,
                            onOpenChapter = { startMs ->
                                val t = state.transcript ?: return@ChaptersTab

                                onOpenSource(
                                    t.id,
                                    t.mediaType,
                                    startMs
                                )
                            }
                        )

                    StudyTab.FLASHCARDS ->
                        FlashcardsTab(
                            state = state,
                            onFlip = viewModel::flipFlashcard,
                            onPrevious = viewModel::previousFlashcard,
                            onNext = viewModel::nextFlashcard,
                            onShuffle = viewModel::shuffleFlashcards,
                            onKnown = {
                                viewModel.markCurrentCard(
                                    FlashcardStatus.KNOWN
                                )
                            },
                            onReview = {
                                viewModel.markCurrentCard(
                                    FlashcardStatus.REVIEW
                                )
                            }
                        )

                    StudyTab.QUIZ ->
                        QuizTab(
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
    Column(
        modifier = modifier.padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.padding(26.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Turn this transcript into a study pack",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Get key points, chapters, flashcards and a quiz. Everything stays on your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        when (nanoState) {
            NanoFeatureState.Available -> {
                AssistChip(
                    onClick = {},
                    label = {
                        Text("On-device AI ready")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null
                        )
                    }
                )
            }

            NanoFeatureState.Downloadable -> {
                FilledTonalButton(
                    onClick = onDownloadNano,
                    enabled = !generating
                ) {
                    Icon(
                        Icons.Default.Download,
                        null
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text("Download on-device AI")
                }
            }

            NanoFeatureState.Downloading -> {
                Text(
                    "Downloading on-device AI…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                AssistChip(
                    onClick = {},
                    label = {
                        Text("Classic offline ready")
                    }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onGenerate,
            enabled = !generating
        ) {
            Text(
                if (generating) {
                    "Generating…"
                } else {
                    "Create study pack"
                }
            )
        }

        if (nanoState is NanoFeatureState.Available) {
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onGenerateClassic,
                enabled = !generating
            ) {
                Text("Use fast Classic mode")
            }
        }
    }
}
