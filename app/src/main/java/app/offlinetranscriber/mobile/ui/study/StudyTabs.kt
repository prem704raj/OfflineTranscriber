package app.offlinetranscriber.mobile.ui.study

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.data.model.StudyPackWithContent
import app.offlinetranscriber.mobile.domain.model.TranscriptSegment
import app.offlinetranscriber.mobile.study.StudyUiState
import app.offlinetranscriber.mobile.study.model.StudyEngineType
import app.offlinetranscriber.mobile.ui.accessibility.minimumTouchTarget
import app.offlinetranscriber.mobile.ui.layout.ReadingWidthContainer

@Composable
fun OverviewTab(
    pack: StudyPackWithContent
) {
    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (
                                pack.pack.engine ==
                                StudyEngineType.GEMINI_NANO.name
                            ) {
                                "Generated on your device"
                            } else {
                                "Classic offline study tools"
                            }
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Key points",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                count = pack.keyPoints.size,
                key = {
                    pack.keyPoints
                        .sortedBy { p -> p.position }[it].id
                }
            ) { index ->
                val point =
                    pack.keyPoints
                        .sortedBy { it.position }[index]

                Surface(
                    color = MaterialTheme.colorScheme
                        .surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.padding(7.dp))

                        Text(
                            text = point.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChaptersTab(
    pack: StudyPackWithContent,
    onOpenChapter: (Long) -> Unit
) {
    val chapters =
        pack.chapters.sortedBy { it.position }

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chapters.isEmpty()) {
                item {
                    EmptyStudySection(
                        "No chapters were generated."
                    )
                }
            }

            items(
                count = chapters.size,
                key = { chapters[it].id }
            ) { index ->
                val chapter = chapters[index]

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenChapter(
                                chapter.startMs
                            )
                        },
                    color = MaterialTheme.colorScheme
                        .surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(17.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onOpenChapter(
                                    chapter.startMs
                                )
                            },
                            label = {
                                Text(
                                    TranscriptSegment.formatTime(
                                        chapter.startMs
                                    )
                                )
                            },
                            modifier = Modifier.minimumTouchTarget()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = chapter.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant
                        )
                    }
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

    val currentId =
        state.flashcardOrder
            .getOrNull(state.flashcardIndex)

    val card = currentId?.let(byId::get)

    if (card == null) {
        EmptyStudySection(
            "No flashcards were generated."
        )
        return
    }

    ReadingWidthContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.flashcardIndex + 1} / ${state.flashcardOrder.size}",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = onShuffle,
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        null
                    )
                    Spacer(Modifier.padding(3.dp))
                    Text("Shuffle")
                }
            }

            Spacer(Modifier.height(18.dp))

            val showingBack = state.flashcardShowingBack
            val accessibilityLabel =
                if (showingBack) {
                    "Answer. ${card.back}. Tap to show question."
                } else {
                    "Question. ${card.front}. Tap to reveal answer."
                }

            AnimatedContent(
                targetState = showingBack,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith
                        fadeOut(tween(120))
                },
                label = "flashcard-side"
            ) { isBack ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp)
                        .clickable(onClick = onFlip)
                        .semantics(mergeDescendants = true) {
                            contentDescription = accessibilityLabel
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (isBack) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isBack) card.back else card.front,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    null
                                )
                                Spacer(Modifier.padding(3.dp))
                                Text(
                                    if (isBack) {
                                        "Tap to see question"
                                    } else {
                                        "Tap to reveal"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Previous")
                }

                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Next")
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onReview,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Text("Review")
                }

                Button(
                    onClick = onKnown,
                    modifier = Modifier.weight(1f).minimumTouchTarget()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null
                    )
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
    val questions =
        state.pack
            ?.quizQuestions
            ?.sortedBy { it.position }
            .orEmpty()

    if (questions.isEmpty()) {
        EmptyStudySection(
            "Not enough distinct study material was available to build a reliable multiple-choice quiz."
        )
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
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Quiz complete",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${attempt.score} / ${questions.size}",
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier.minimumTouchTarget()
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        null
                    )
                    Spacer(Modifier.padding(3.dp))
                    Text("Try again")
                }
            }
            return@ReadingWidthContainer
        }

        val question =
            questions.getOrNull(
                attempt.questionIndex
            ) ?: return@ReadingWidthContainer

        val options = question.options()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Question ${attempt.questionIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(22.dp))

            options.forEachIndexed { index, option ->
                val selected =
                    attempt.selectedIndex == index

                val correct =
                    question.correctIndex == index

                val label =
                    when {
                        !attempt.answered -> option
                        selected && correct ->
                            "Correct — $option"
                        selected && !correct ->
                            "Incorrect — $option"
                        correct ->
                            "Correct answer — $option"
                        else -> option
                    }

                val container =
                    if (
                        attempt.answered &&
                        correct
                    ) {
                        MaterialTheme.colorScheme
                            .primaryContainer
                    } else if (
                        attempt.answered &&
                        selected &&
                        !correct
                    ) {
                        MaterialTheme.colorScheme
                            .errorContainer
                    } else {
                        MaterialTheme.colorScheme
                            .surfaceVariant.copy(alpha = 0.45f)
                    }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !attempt.answered
                        ) {
                            onAnswer(index)
                        },
                    color = container,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(Modifier.height(9.dp))
            }

            if (attempt.answered) {
                Spacer(Modifier.height(10.dp))

                Surface(
                    color = MaterialTheme.colorScheme
                        .secondaryContainer,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Explanation",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(question.explanation)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumTouchTarget()
                ) {
                    Text(
                        if (
                            attempt.questionIndex ==
                            questions.lastIndex
                        ) {
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
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
