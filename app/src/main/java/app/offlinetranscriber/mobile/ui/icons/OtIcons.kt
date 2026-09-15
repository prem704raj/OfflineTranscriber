package app.offlinetranscriber.mobile.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object OtIcons {

    val RecordWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "RecordWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 12f)
                lineTo(6.5f, 12f)
                lineTo(8f, 7f)
                lineTo(10f, 17f)
                lineTo(12f, 9f)
                lineTo(14f, 15f)
                lineTo(16f, 12f)
                lineTo(20f, 12f)
            }
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                moveTo(3f, 5f)
                arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
                arcToRelative(2f, 2f, 0f, true, true, -4f, 0f)
            }
        }.build()
    }

    val Transcript: ImageVector by lazy {
        ImageVector.Builder(
            name = "Transcript",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                close()

                moveTo(8f, 9f)
                lineTo(16f, 9f)

                moveTo(8f, 13f)
                lineTo(16f, 13f)

                moveTo(8f, 17f)
                lineTo(13f, 17f)
            }
        }.build()
    }

    val AudioImport: ImageVector by lazy {
        ImageVector.Builder(
            name = "AudioImport",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 15f)
                lineTo(5f, 7f)
                lineTo(15f, 5f)
                lineTo(15f, 15f)

                moveTo(15f, 8f)
                lineTo(19f, 8f)

                moveTo(17f, 6f)
                lineTo(19f, 8f)
                lineTo(17f, 10f)

                moveTo(5f, 15f)
                arcToRelative(3f, 2f, 0f, true, false, 0f, 4f)

                moveTo(15f, 15f)
                arcToRelative(3f, 2f, 0f, true, false, 0f, 4f)
            }
        }.build()
    }

    val VideoImport: ImageVector by lazy {
        ImageVector.Builder(
            name = "VideoImport",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                lineTo(15f, 6f)
                lineTo(15f, 18f)
                lineTo(4f, 18f)
                close()

                moveTo(15f, 10f)
                lineTo(20f, 7.5f)
                lineTo(20f, 16.5f)
                lineTo(15f, 14f)

                moveTo(8f, 12f)
                lineTo(11f, 12f)

                moveTo(9.5f, 10.5f)
                lineTo(11f, 12f)
                lineTo(9.5f, 13.5f)
            }
        }.build()
    }

    val SearchTimeline: ImageVector by lazy {
        ImageVector.Builder(
            name = "SearchTimeline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 4f)
                arcToRelative(6.5f, 6.5f, 0f, true, true, 0f, 13f)
                arcToRelative(6.5f, 6.5f, 0f, true, true, 0f, -13f)

                moveTo(15.3f, 15.3f)
                lineTo(20f, 20f)

                moveTo(7.5f, 10.5f)
                lineTo(9f, 10.5f)
                lineTo(10f, 8.5f)
                lineTo(11f, 12.5f)
                lineTo(12.5f, 10.5f)
                lineTo(14f, 10.5f)
            }
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "Library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 4f)
                lineTo(8.5f, 4f)
                lineTo(8.5f, 20f)
                lineTo(5f, 20f)
                close()

                moveTo(10.5f, 5f)
                lineTo(14f, 5f)
                lineTo(14f, 20f)
                lineTo(10.5f, 20f)
                close()

                moveTo(16f, 7f)
                lineTo(19f, 6f)
                lineTo(21f, 19f)
                lineTo(18f, 20f)
                close()
            }
        }.build()
    }

    val Processing: ImageVector by lazy {
        ImageVector.Builder(
            name = "Processing",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 8f)
                lineTo(8f, 8f)
                lineTo(9.5f, 4.5f)
                lineTo(11.5f, 13.5f)
                lineTo(13f, 9f)
                lineTo(14.5f, 12f)
                lineTo(16f, 8f)
                lineTo(19f, 8f)

                moveTo(5f, 17f)
                lineTo(19f, 17f)
            }
        }.build()
    }

    val Evidence: ImageVector by lazy {
        ImageVector.Builder(
            name = "Evidence",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 15f)
                lineTo(12f, 15f)
                lineTo(8f, 19f)
                lineTo(8f, 15f)
                lineTo(5f, 15f)
                close()

                moveTo(8f, 9f)
                lineTo(16f, 9f)

                moveTo(8f, 12f)
                lineTo(13f, 12f)
            }
        }.build()
    }

    val Study: ImageVector by lazy {
        ImageVector.Builder(
            name = "Study",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                lineTo(11f, 4f)
                lineTo(11f, 18f)
                lineTo(4f, 20f)
                close()

                moveTo(20f, 6f)
                lineTo(13f, 4f)
                lineTo(13f, 18f)
                lineTo(20f, 20f)
                close()
            }
        }.build()
    }

    val Speaker: ImageVector by lazy {
        ImageVector.Builder(
            name = "Speaker",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 9f)
                lineTo(8f, 9f)
                lineTo(12f, 5f)
                lineTo(12f, 19f)
                lineTo(8f, 15f)
                lineTo(4f, 15f)
                close()

                moveTo(15f, 9f)
                curveTo(17f, 10f, 17f, 14f, 15f, 15f)

                moveTo(17.5f, 6.5f)
                curveTo(21f, 9f, 21f, 15f, 17.5f, 17.5f)
            }
        }.build()
    }

    val Export: ImageVector by lazy {
        ImageVector.Builder(
            name = "Export",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 10f)
                lineTo(5f, 19f)
                lineTo(19f, 19f)
                lineTo(19f, 10f)

                moveTo(12f, 15f)
                lineTo(12f, 4f)

                moveTo(8f, 8f)
                lineTo(12f, 4f)
                lineTo(16f, 8f)
            }
        }.build()
    }

    val LocalShield: ImageVector by lazy {
        ImageVector.Builder(
            name = "LocalShield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                lineTo(19f, 6.5f)
                lineTo(18f, 14f)
                curveTo(17.5f, 17.5f, 15f, 19.5f, 12f, 21f)
                curveTo(9f, 19.5f, 6.5f, 17.5f, 6f, 14f)
                lineTo(5f, 6.5f)
                close()

                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15.5f, 9.5f)
            }
        }.build()
    }

    val Collection: ImageVector by lazy {
        ImageVector.Builder(
            name = "Collection",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                lineTo(10f, 6f)
                lineTo(12f, 8f)
                lineTo(20f, 8f)
                lineTo(20f, 19f)
                lineTo(4f, 19f)
                close()
            }
        }.build()
    }

    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 4f)
                lineTo(17f, 4f)
                lineTo(17f, 20f)
                lineTo(12f, 16.5f)
                lineTo(7f, 20f)
                close()
            }
        }.build()
    }
}
