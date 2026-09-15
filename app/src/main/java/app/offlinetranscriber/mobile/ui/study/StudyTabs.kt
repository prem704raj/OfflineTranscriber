package app.offlinetranscriber.mobile.ui.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.model.StudyPackWithContent
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.study.StudyUiState
import app.offlinetranscriber.mobile.study.model.StudyEngineType
import app.offlinetranscriber.mobile.theme.OtPlaybackTimeStyle
import app.offlinetranscriber.mobile.theme.OtTranscriptBodyStyle
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget
import app.offlinetranscriber.mobile.ui.design.AppDimens
import app.offlinetranscriber.mobile.ui.design.AppShapes
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer
import app.offlinetranscriber.mobile.ui.system.OtSectionHeader
import app.offlinetranscriber.mobile.ui.system.OtStatusKind
import app.offlinetranscriber.mobile.ui.system.OtStatusLabel

@Composable
fun OverviewTab(
    pack: StudyPackWithContent
) {
    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = AppDimens.ScreenHorizontal,
                vertical = AppDimens.Space4
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space3)
        ) {
            item {
                val labelText = if (pack.pack.engine == StudyEngineType.GEMINI_NANO.name) {
                    "Generated on your device"
                } else {
                    "Classic offline study tools"
                }
                OtStatusLabel(
                    text = labelText,
                    kind = OtStatusKind.LOCAL
                )

                Spacer(Modifier.height(AppDimens.Space3))

                OtSectionHeader(title = "Key points")
            }

            items(
                count = pack.keyPoints.size,
                key = { pack.keyPoints.sortedBy { p -> p.position }[it].id }
            ) { index ->
                val point = pack.keyPoints.sortedBy { p -> p.position }[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )

                    Text(
                        text = point.text,
                        style = OtTranscriptBodyStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun ChaptersTab(
    pack: StudyPackWithContent,
    onOpenChapter: (Long) -> Unit
) {
    val chapters = pack.chapters.sortedBy { it.startMs }

    if (chapters.isEmpty()) {
        EmptyStudySection("No chapter breakdown was generated for this recording.")
        return
    }

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = AppDimens.ScreenHorizontal,
                vertical = AppDimens.Space4
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space2)
        ) {
            items(chapters, key = { it.id }) { chapter ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            onOpenChapter(chapter.startMs)
                        }
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(AppDimens.TranscriptGutterWidth)
                                .padding(top = 2.dp)
                        ) {
                            Text(
                                text = TranscriptSegment.formatTime(chapter.startMs),
                                style = OtPlaybackTimeStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = chapter.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun FlashcardsTab(
    state: StudyUiState,
    onFlip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onKnown: () -> Unit,
    onReview: () -> Unit
) {
    val pack = state.pack ?: return
    val byId = pack.flashcards.associateBy { it.id }
    val currentId = state.flashcardOrder.getOrNull(state.flashcardIndex)
    val card = currentId?.let(byId::get)

    if (card == null) {
        EmptyStudySection("No flashcards were generated.")
        return
    }

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.flashcardIndex + 1} / ${state.flashcardOrder.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = onShuffle,
                    shape = AppShapes.Control,
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("Shuffle")
                }
            }

            Spacer(Modifier.height(AppDimens.Space3))

            val showingBack = state.flashcardShowingBack
            val accessibilityLabel = if (showingBack) {
                "Answer. ${card.back}. Tap to show question."
            } else {
                "Question. ${card.front}. Tap to reveal answer."
            }

            AnimatedContent(
                targetState = showingBack,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "flashcard-side"
            ) { isBack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = AppShapes.Hero
                        )
                        .clickable(role = Role.Button, onClick = onFlip)
                        .semantics(mergeDescendants = true) {
                            contentDescription = accessibilityLabel
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBack) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    shape = AppShapes.Hero
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isBack) card.back else card.front,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.padding(3.dp))
                                Text(
                                    text = if (isBack) "Tap to see question" else "Tap to reveal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(AppDimens.Space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    shape = AppShapes.Button,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Previous")
                }

                OutlinedButton(
                    onClick = onNext,
                    shape = AppShapes.Button,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Next")
                }
            }

            Spacer(Modifier.height(AppDimens.Space2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onReview,
                    shape = AppShapes.Button,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Review")
                }

                Button(
                    onClick = onKnown,
                    shape = AppShapes.Button,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("Known")
                }
            }
        }
    }
}

@Composable
fun QuizTab(
    state: StudyUiState,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit
) {
    val questions = state.pack
        ?.quizQuestions
        ?.sortedBy { it.position }
        .orEmpty()

    if (questions.isEmpty()) {
        EmptyStudySection("Not enough distinct study material was available to build a reliable multiple-choice quiz.")
        return
    }

    val attempt = state.quiz

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        if (attempt.finished) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(8.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Quiz complete",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${attempt.score} / ${questions.size}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onRetry,
                    shape = AppShapes.Button,
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("Try again")
                }
            }
            return@ReadingWidthContainer
        }

        val question = questions.getOrNull(attempt.questionIndex) ?: return@ReadingWidthContainer
        val options = question.options()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.ScreenHorizontal)
        ) {
            Text(
                text = "Question ${attempt.questionIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(18.dp))

            options.forEachIndexed { index, option ->
                val selected = attempt.selectedIndex == index
                val correct = question.correctIndex == index

                val label = when {
                    !attempt.answered -> option
                    selected && correct -> "Correct — $option"
                    selected && !correct -> "Incorrect — $option"
                    correct -> "Correct answer — $option"
                    else -> option
                }

                val containerColor = if (attempt.answered && correct) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else if (attempt.answered && selected && !correct) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }

                val borderModifier = Modifier.border(
                    width = 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = AppShapes.Control
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(borderModifier)
                        .clickable(enabled = !attempt.answered) {
                            onAnswer(index)
                        },
                    color = containerColor,
                    shape = AppShapes.Control
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            if (attempt.answered) {
                Spacer(Modifier.height(10.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = AppShapes.Control
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Explanation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onNext,
                    shape = AppShapes.Button,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                ) {
                    Text(
                        if (attempt.questionIndex == questions.lastIndex) {
                            "See score"
                        } else {
                            "Next question"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStudySection(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
