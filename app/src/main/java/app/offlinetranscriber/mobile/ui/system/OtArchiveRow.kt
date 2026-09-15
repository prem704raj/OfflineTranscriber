package app.offlinetranscriber.mobile.ui.system

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.offlinetranscriber.mobile.theme.OtMeasuredDataStyle

@Composable
fun OtArchiveRow(
    icon: ImageVector,
    title: String,
    snippet: String?,
    primaryMeta: String?,
    secondaryMeta: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!snippet.isNullOrBlank()) {
                    Spacer(Modifier.size(3.dp))

                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!primaryMeta.isNullOrBlank() || !secondaryMeta.isNullOrBlank()) {
                    Spacer(Modifier.size(7.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!primaryMeta.isNullOrBlank()) {
                            Text(
                                text = primaryMeta,
                                style = OtMeasuredDataStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!secondaryMeta.isNullOrBlank()) {
                            if (!primaryMeta.isNullOrBlank()) {
                                Spacer(Modifier.size(12.dp))
                            }

                            Text(
                                text = secondaryMeta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            trailing?.let {
                Spacer(Modifier.size(8.dp))
                it()
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
