package app.offlinetranscriber.mobile.ui.system

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.theme.OtPlaybackTimeStyle
import app.offlinetranscriber.mobile.theme.OtTranscriptBodyStyle
import app.offlinetranscriber.mobile.ui.design.AppDimens

@Composable
fun OtTranscriptLine(
    timestamp: String,
    text: String,
    active: Boolean,
    bookmarked: Boolean,
    speakerLabel: String?,
    speakerColor: Color?,
    onSeek: () -> Unit,
    onLongPressOrEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val railColor by animateColorAsState(
        targetValue =
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
        animationSpec = tween(durationMillis = 140),
        label = "transcriptPlayhead"
    )

    val timestampColor =
        if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Button,
                onClick = onSeek
            )
            .semantics {
                if (active) {
                    stateDescription = "Currently playing"
                }
            },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(AppDimens.TranscriptGutterWidth)
                .padding(
                    top = 16.dp,
                    end = 12.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(AppDimens.TranscriptRailWidth)
                        .heightIn(min = 22.dp)
                        .background(railColor)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = timestamp,
                    style = OtPlaybackTimeStyle.copy(
                        fontWeight =
                            if (active) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                    ),
                    color = timestampColor
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = 13.dp,
                    bottom = 15.dp
                )
        ) {
            if (!speakerLabel.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (speakerColor != null) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .heightIn(min = 3.dp)
                                .background(
                                    speakerColor,
                                    MaterialTheme.shapes.extraSmall
                                )
                        )
                        Spacer(Modifier.width(7.dp))
                    }

                    Text(
                        text = speakerLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(5.dp))
            }

            Text(
                text = text,
                style = OtTranscriptBodyStyle,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (bookmarked) {
                Spacer(Modifier.width(6.dp))

                Text(
                    text = "●",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
