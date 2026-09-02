package com.example.transcriber.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.example.transcriber.ui.design.AppDimens

enum class AppWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun appWidthClass(): AppWidthClass {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width < 600 -> AppWidthClass.COMPACT
        width < 840 -> AppWidthClass.MEDIUM
        else -> AppWidthClass.EXPANDED
    }
}

@Composable
fun ReadingWidthContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(max = AppDimens.ReadingMaxWidth)
                .padding(horizontal = AppDimens.ScreenHorizontal)
        ) {
            content()
        }
    }
}
