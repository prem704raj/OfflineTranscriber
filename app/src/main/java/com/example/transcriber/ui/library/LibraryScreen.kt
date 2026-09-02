package com.example.transcriber.ui.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.transcriber.data.model.BookmarkMomentRow
import com.example.transcriber.data.model.CollectionSummaryRow
import com.example.transcriber.data.model.LibraryTranscriptRow
import com.example.transcriber.data.model.MediaType
import com.example.transcriber.domain.model.TranscriptSegment
import com.example.transcriber.library.LibraryViewModel
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
    onOpenPaywall: (com.example.transcriber.billing.ProFeature) -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state.openPaywallFeature) {
        state.openPaywallFeature?.let { feature ->
            onOpenPaywall(feature)
            viewModel.clearPaywallTrigger()
        }
    }

    var creatingCollection by remember {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Organize transcripts into collections, search bookmarks, and ask questions across all recordings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAskLibrary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ask Library")
                }

                FilledTonalButton(
                    onClick = {
                        creatingCollection = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("New Collection")
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle(
                title = "Collections",
                icon = Icons.Default.FolderOpen
            )

            Spacer(Modifier.height(10.dp))

            if (state.collections.isEmpty()) {
                EmptyCollectionCard()
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

            Spacer(Modifier.height(26.dp))

            SectionTitle(
                title = "Bookmarked Moments",
                icon = Icons.Default.Bookmark
            )

            Spacer(Modifier.height(10.dp))
        }

        if (state.recentBookmarks.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Bookmark an important moment from any transcript or subtitle and it will appear here.",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(
                items = state.recentBookmarks,
                key = { "bookmark_${it.bookmarkId}" }
            ) { bookmark ->
                BookmarkMomentCard(
                    item = bookmark,
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
            Spacer(Modifier.height(12.dp))

            SectionTitle(
                title = "All Transcripts",
                icon = Icons.Default.LibraryBooks
            )
        }

        items(
            items = state.transcripts,
            key = { "transcript_${it.id}" }
        ) { transcript ->
            LibraryTranscriptCard(
                transcript = transcript,
                onOpen = {
                    onOpenTranscript(
                        transcript.id,
                        transcript.mediaType,
                        null
                    )
                },
                onCollections = {
                    viewModel.openCollectionPicker(transcript.id)
                }
            )
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
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun EmptyCollectionCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = "Collections keep related lectures, meetings, and videos organized together.",
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .width(210.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { menu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Collection options"
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

            Spacer(Modifier.height(14.dp))

            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${collection.transcriptCount} transcripts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun BookmarkMomentCard(
    item: BookmarkMomentRow,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(TranscriptSegment.formatTime(item.startMs))
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun LibraryTranscriptCard(
    transcript: LibraryTranscriptRow,
    onOpen: () -> Unit,
    onCollections: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (transcript.mediaType == MediaType.VIDEO) {
                    Icons.Default.Movie
                } else {
                    Icons.Default.Audiotrack
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transcript.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = buildString {
                        append(TranscriptSegment.formatTime(transcript.durationMs))
                        append(" • ")
                        append(SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(transcript.createdAt)))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (transcript.bookmarkCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${transcript.bookmarkCount} bookmark${if (transcript.bookmarkCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onCollections
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Add to collections"
                )
            }
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
                placeholder = { Text("e.g. DBMS") },
                shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(10.dp)
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
