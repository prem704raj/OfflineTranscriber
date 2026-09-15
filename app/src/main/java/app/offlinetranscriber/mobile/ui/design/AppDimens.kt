package app.offlinetranscriber.mobile.ui.design

import androidx.compose.ui.unit.dp

object AppDimens {
    // 4dp base rhythm
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp

    // Backward-compatible aliases while the migration is in progress.
    // Remove old aliases only after all screens use the semantic values.
    val GapXs = Space2
    val GapSm = Space3
    val GapMd = Space4
    val GapLg = Space6
    val GapXl = Space8

    val PhoneCompactHorizontal = 16.dp
    val PhoneHorizontal = 20.dp
    val TabletHorizontal = 24.dp

    val ScreenHorizontal = PhoneHorizontal
    val ScreenVertical = 20.dp

    val MinTouchTarget = 48.dp
    val PrimaryTouchTarget = 56.dp

    val ListRowVertical = 14.dp
    val ListRowHorizontal = 0.dp

    val TranscriptGutterWidth = 66.dp
    val TranscriptRailWidth = 3.dp

    val DividerThickness = 1.dp

    val ReadingMaxWidth = 760.dp
    val FormMaxWidth = 680.dp
    val UtilityMaxWidth = 720.dp

    // Kept temporarily to avoid compile regressions during migration.
    val CardPadding = 16.dp
    val LargeCardPadding = 20.dp
    val CardRadius = 12.dp
    val LargeCardRadius = 16.dp
}
