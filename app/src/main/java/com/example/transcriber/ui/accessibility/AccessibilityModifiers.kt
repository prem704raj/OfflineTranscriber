package com.example.transcriber.ui.accessibility

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

fun Modifier.minimumTouchTarget(): Modifier =
    this.sizeIn(
        minWidth = 48.dp,
        minHeight = 48.dp
    )

fun Modifier.accessibleAction(
    label: String
): Modifier =
    this
        .minimumTouchTarget()
        .semantics {
            contentDescription = label
        }

fun Modifier.progressSemantics(
    percent: Int
): Modifier =
    this.semantics {
        progressBarRangeInfo =
            ProgressBarRangeInfo(
                percent.coerceIn(0, 100).toFloat(),
                0f..100f,
                100
            )
    }
