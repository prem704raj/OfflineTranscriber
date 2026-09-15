package app.offlinetranscriber.mobile.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun screenHorizontalPadding(
    width: Dp
): Dp =
    when {
        width < 360.dp ->
            AppDimens.PhoneCompactHorizontal

        width < 600.dp ->
            AppDimens.PhoneHorizontal

        else ->
            AppDimens.TabletHorizontal
    }
