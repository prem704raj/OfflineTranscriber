package app.offlinetranscriber.mobile.ui.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.ask.model.AskScope
import app.offlinetranscriber.mobile.billing.ProFeature
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskScreen(
    onBack: () -> Unit,
    onOpenSource: (transcriptId: Long, mediaType: String, seekMs: Long) -> Unit,
    onOpenPaywall: (ProFeature) -> Unit,
    onExport: (conversationId: Long) -> Unit = {},
    viewModel: AskViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.messages.size, state.asking) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.scope == AskScope.TRANSCRIPT) {
                                "Ask this transcript"
                            } else {
                                "Ask your library"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.semantics { heading() }
                        )
                        if (state.scope == AskScope.TRANSCRIPT && !state.transcriptTitle.isNullOrBlank()) {
                            Text(
                                text = state.transcriptTitle.orEmpty(),
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
                        modifier = Modifier.minimumTouchTarget()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        state.conversationId?.let { convId ->
                            IconButton(
                                onClick = { onExport(convId) },
                                modifier = Modifier.minimumTouchTarget()
                            ) {
                                Icon(
                                    imageVector = OtIcons.Export,
                                    contentDescription = "Export & Share"
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.minimumTouchTarget()
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear conversation"
                            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Messages List
                Box(modifier = Modifier.weight(1f)) {
                    if (state.messages.isEmpty() && !state.asking) {
                        AskEmptyState(
                            scope = state.scope,
                            onSelectSuggestion = { suggestion ->
                                viewModel.askQuestion(
                                    customText = suggestion,
                                    onProRequired = {
                                        onOpenPaywall(ProFeature.ASK_TRANSCRIPTS)
                                    }
                                )
                            },
                            modifier = Modifier.padding(horizontal = AppDimens.ScreenHorizontal)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = AppDimens.ScreenHorizontal,
                                vertical = AppDimens.Space3
                            ),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.messages,
                                key = { it.id }
                            ) { message ->
                                AskMessageCard(
                                    message = message,
                                    onOpenCitation = { citation ->
                                        onOpenSource(
                                            citation.transcriptId,
                                            citation.mediaType,
                                            citation.startMs
                                        )
                                    }
                                )
                            }

                            if (state.asking) {
                                item(key = "loading_indicator") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = AppShapes.Control,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = "Searching your transcripts…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Question Input Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppDimens.ScreenHorizontal,
                                vertical = AppDimens.Space2
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.question,
                            onValueChange = { viewModel.onQuestionChange(it) },
                            placeholder = {
                                Text(
                                    if (state.scope == AskScope.TRANSCRIPT) {
                                        "Ask about this transcript…"
                                    } else {
                                        "Ask anything across your library…"
                                    }
                                )
                            },
                            maxLines = 4,
                            shape = AppShapes.Control,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                viewModel.askQuestion(
                                    onProRequired = {
                                        onOpenPaywall(ProFeature.ASK_TRANSCRIPTS)
                                    }
                                )
                            },
                            enabled = state.question.trim().length >= 2 && !state.asking,
                            modifier = Modifier.minimumTouchTarget()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Ask question",
                                tint = if (state.question.trim().length >= 2 && !state.asking) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
