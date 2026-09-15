package app.offlinetranscriber.mobile.ui.system

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun OtAmplitudeMeter(
    amplitude: Int,
    modifier: Modifier = Modifier
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val bars = 22
        val gap = size.width / bars
        val normalized = (amplitude / 32767f).coerceIn(0f, 1f)

        repeat(bars) { index ->
            val centerDistance =
                kotlin.math.abs(index - (bars - 1) / 2f) / ((bars - 1) / 2f)

            val envelope = 1f - centerDistance * 0.55f

            val barFactor =
                ((index * 37) % 11) / 10f * 0.35f + 0.65f

            val amount = max(
                0.09f,
                normalized * envelope * barFactor
            )

            val h = size.height * amount
            val x = gap * index + gap / 2f

            drawLine(
                color = if (amount > 0.16f) active else inactive,
                start = Offset(x, size.height / 2f - h / 2f),
                end = Offset(x, size.height / 2f + h / 2f),
                strokeWidth = minOf(4.dp.toPx(), gap * 0.42f),
                cap = StrokeCap.Round
            )
        }
    }
}
