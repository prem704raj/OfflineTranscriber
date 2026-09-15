package app.offlinetranscriber.mobile.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.offlinetranscriber.mobile.data.model.CollectionSummaryRow
import app.offlinetranscriber.mobile.data.model.MediaType
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.library.LibraryViewModel
import app.offlinetranscriber.mobile.ui.accessibility.accessibleAction
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.icons.OtIcons
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtArchiveRow
import app.offlinetranscriber.mobile.ui.system.OtEmptyState
import app.offlinetranscriber.mobile.ui.system.OtSectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    onOpenCollection: (Long) -> Unit,
    onOpenTranscript: (
        transcriptId: Long,
        mediaType: String,
        seekMs: Long?
    ) -> Unit,
    onAskLibrary: () -> Unit = {},
    onOpenPaywall: (app.offlinetranscriber.mobile.billing.ProFeature) -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.openPaywallFeature) {
        state.openPaywallFeature?.let { feature ->
            onOpenPaywall(feature)
            viewModel.clearPaywallTrigger()
        }
    }

    var creatingCollection by remember {
        mutableStateOf(false)
    }

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.ScreenHorizontal,
                end = AppDimens.ScreenHorizontal,
                top = AppDimens.Space6,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            item {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(AppDimens.Space1))

                Text(
                    text = "Organize transcripts into collections, search bookmarks, and ask questions across all recordings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(AppDimens.Space4))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAskLibrary,
                        shape = AppShapes.Button
                    ) {
                        Icon(
                            imageVector = OtIcons.Evidence,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Ask Library")
                    }

                    FilledTonalButton(
                        onClick = {
                            creatingCollection = true
                        },
                        shape = AppShapes.Button
                    ) {
                        Icon(
                            imageVector = OtIcons.Collection,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("New Collection")
                    }
                }

                Spacer(Modifier.height(AppDimens.Space5))

                OtSectionHeader(
                    title = "Collections"
                )

                Spacer(Modifier.height(AppDimens.Space2))

                if (state.collections.isEmpty()) {
                    Text(
                        text = "No collections yet. Group related recordings together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space3)
                    ) {
                        items(
                            items = state.collections,
                            key = { it.id }
                        ) { collection ->
                            CollectionCard(
                                collection = collection,
                                onClick = {
                                    onOpenCollection(collection.id)
                                },
                                onDelete = {
                                    viewModel.deleteCollection(collection.id)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(AppDimens.Space5))

                OtSectionHeader(
                    title = "Bookmarked Moments"
                )

                Spacer(Modifier.height(AppDimens.Space1))
            }

            if (state.recentBookmarks.isEmpty()) {
                item {
                    Text(
                        text = "Bookmark an important moment from any transcript and it will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = AppDimens.Space2)
                    )
                }
            } else {
                items(
                    items = state.recentBookmarks,
                    key = { "bookmark_${it.bookmarkId}" }
                ) { bookmark ->
                    OtArchiveRow(
                        icon = OtIcons.Bookmark,
                        title = bookmark.title,
                        snippet = bookmark.text,
                        primaryMeta = TranscriptSegment.formatTime(bookmark.startMs),
                        secondaryMeta = if (bookmark.mediaType == MediaType.VIDEO) "Video" else "Audio",
                        onClick = {
                            onOpenTranscript(
                                bookmark.transcriptId,
                                bookmark.mediaType,
                                bookmark.startMs
                            )
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(AppDimens.Space3))

                OtSectionHeader(
                    title = "All Transcripts"
                )
            }

            if (state.transcripts.isEmpty()) {
                item {
                    OtEmptyState(
                        title = "No transcripts yet",
                        body = "Transcripts created on the home screen will be organized in your library."
                    )
                }
            } else {
                items(
                    items = state.transcripts,
                    key = { "transcript_${it.id}" }
                ) { transcript ->
                    val isVideo = transcript.mediaType == MediaType.VIDEO
                    val icon = if (isVideo) OtIcons.VideoImport else OtIcons.Transcript
                    val dateFormatted = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transcript.createdAt))
                    val durationFormatted = TranscriptSegment.formatTime(transcript.durationMs)
                    val bookmarkText = if (transcript.bookmarkCount > 0) {
                        "${transcript.bookmarkCount} bookmark${if (transcript.bookmarkCount > 1) "s" else ""}"
                    } else null

                    OtArchiveRow(
                        icon = icon,
                        title = transcript.title,
                        snippet = null,
                        primaryMeta = durationFormatted,
                        secondaryMeta = if (bookmarkText != null) "$dateFormatted • $bookmarkText" else dateFormatted,
                        onClick = {
                            onOpenTranscript(
                                transcript.id,
                                transcript.mediaType,
                                null
                            )
                        },
                        trailing = {
                            IconButton(
                                onClick = { viewModel.openCollectionPicker(transcript.id) },
                                modifier = Modifier.accessibleAction("Add to collections")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (creatingCollection) {
        NewCollectionDialog(
            onDismiss = {
                creatingCollection = false
            },
            onCreate = { name ->
                viewModel.createCollection(name)
                creatingCollection = false
            }
        )
    }

    state.collectionPicker?.let { picker ->
        CollectionPickerDialog(
            collections = state.collections,
            selectedIds = picker.selectedCollectionIds,
            onToggle = viewModel::setCollectionMembership,
            onDismiss = viewModel::closeCollectionPicker
        )
    }
}

@Composable
private fun CollectionCard(
    collection: CollectionSummaryRow,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = AppShapes.Button
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = OtIcons.Collection,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { menu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Collection options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Delete collection")
                            },
                            onClick = {
                                menu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${collection.transcriptCount} transcripts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New collection", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.take(60) },
                singleLine = true,
                label = { Text("Collection name") },
                placeholder = { Text("e.g. Lectures") },
                shape = AppShapes.Control,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onCreate(value.trim())
                    }
                },
                enabled = value.isNotBlank(),
                shape = AppShapes.Button
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
