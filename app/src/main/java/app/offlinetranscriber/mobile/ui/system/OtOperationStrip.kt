package app.offlinetranscriber.mobile.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.theme.OtMeasuredDataStyle

@Composable
fun OtOperationStrip(
    title: String,
    progress: Int?,
    secondary: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val clickModifier =
        if (onClick != null) {
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
        } else {
            Modifier
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )

            Spacer(Modifier.size(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (progress != null) {
                Text(
                    text = "${progress.coerceIn(0, 100)}%",
                    style = OtMeasuredDataStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (progress != null) {
            LinearProgressIndicator(
                progress = {
                    progress.coerceIn(0, 100) / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (!secondary.isNullOrBlank()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
