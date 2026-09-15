package app.offlinetranscriber.mobile.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class OtStatusKind {
    LOCAL,
    PROCESSING,
    NEUTRAL
}

@Composable
fun OtStatusLabel(
    text: String,
    kind: OtStatusKind,
    modifier: Modifier = Modifier
) {
    val dot = when (kind) {
        OtStatusKind.LOCAL ->
            MaterialTheme.colorScheme.secondary

        OtStatusKind.PROCESSING ->
            MaterialTheme.colorScheme.tertiary

        OtStatusKind.NEUTRAL ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = dot,
                    shape = CircleShape
                )
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
