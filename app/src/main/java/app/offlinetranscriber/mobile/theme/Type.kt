package app.offlinetranscriber.mobile.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.offlinetranscriber.mobile.R

val OtBodyFamily = FontFamily(
    Font(
        resId = R.font.noto_sans_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.noto_sans_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.noto_sans_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.noto_sans_bold,
        weight = FontWeight.Bold
    )
)

val OtDisplayFamily = FontFamily(
    Font(
        resId = R.font.noto_sans_display_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.noto_sans_display_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.noto_sans_display_bold,
        weight = FontWeight.Bold
    )
)

val OtMeasuredDataStyle = TextStyle(
    fontFamily = OtDisplayFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum"
)

val OtPlaybackTimeStyle = TextStyle(
    fontFamily = OtDisplayFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum"
)

val OtTranscriptBodyStyle = TextStyle(
    fontFamily = OtBodyFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 27.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.35).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp
    ),

    headlineLarge = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),

    titleLarge = TextStyle(
        fontFamily = OtDisplayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),

    labelLarge = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OtBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
