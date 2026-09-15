package app.offlinetranscriber.mobile.ui.system

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OtWaveToTextMark(
    modifier: Modifier = Modifier
) {
    val line = MaterialTheme.colorScheme.outline
    val active = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier.size(
            width = 124.dp,
            height = 72.dp
        )
    ) {
        val mid = size.height * 0.34f

        val xs = listOf(
            10f, 23f, 36f, 49f, 62f, 75f
        ).map {
            it / 124f * size.width
        }

        val heights = listOf(
            12f, 29f, 18f, 38f, 22f, 14f
        ).map {
            it / 72f * size.height
        }

        xs.forEachIndexed { index, x ->
            drawLine(
                color = if (index == 3) active else line,
                start = Offset(x, mid - heights[index] / 2f),
                end = Offset(x, mid + heights[index] / 2f),
                strokeWidth = if (index == 3) 4f else 3f,
                cap = StrokeCap.Round
            )
        }

        val textStart = size.width * 0.12f
        val y1 = size.height * 0.68f
        val y2 = size.height * 0.84f

        drawLine(
            color = line,
            start = Offset(textStart, y1),
            end = Offset(size.width * 0.88f, y1),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        drawLine(
            color = line,
            start = Offset(textStart, y2),
            end = Offset(size.width * 0.68f, y2),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun OtEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OtWaveToTextMark()

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        action?.let {
            Spacer(Modifier.height(18.dp))
            it()
        }
    }
}
