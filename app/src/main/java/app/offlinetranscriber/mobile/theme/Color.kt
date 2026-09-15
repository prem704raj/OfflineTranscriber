package app.offlinetranscriber.mobile.theme

import androidx.compose.ui.graphics.Color

/*
 * Offline Transcriber — Recorded Paper palette
 *
 * Semantic rule:
 * ActionCobalt   = action / selection / active playback
 * LocalTeal      = verified local/private/safe state
 * ProcessAmber   = queued/running/downloading state
 * Destructive    = destructive/error only
 *
 * Neutrals create hierarchy but carry no semantic meaning.
 */

// Primary semantic colors — light theme
val ActionCobalt = Color(0xFF3157C8)
val ActionCobaltDeep = Color(0xFF2949B0)
val ActionCobaltSoft = Color(0xFFE8EDFF)

val LocalTeal = Color(0xFF08786E)
val LocalTealSoft = Color(0xFFE1F3F0)

val ProcessAmber = Color(0xFFA65D00)
val ProcessAmberSoft = Color(0xFFFFEBD3)

val DestructiveBrick = Color(0xFFB42318)
val DestructiveBrickSoft = Color(0xFFFFE8E5)

// Neutral light system
val CanvasMist = Color(0xFFF3F5F7)
val SheetWhite = Color(0xFFFFFFFF)

val Ink950 = Color(0xFF18202A)
val Ink700 = Color(0xFF46515E)
val Ink500 = Color(0xFF687482)

val Line300 = Color(0xFFD6DCE3)
val Line200 = Color(0xFFE5E9EE)
val UtilityFill = Color(0xFFEDF0F3)

// Dark theme — charcoal document environment, not black
val DarkCanvas = Color(0xFF20252B)
val DarkSheet = Color(0xFF292F36)
val DarkUtility = Color(0xFF333A42)
val DarkLine = Color(0xFF46505A)

val DarkTextPrimary = Color(0xFFF3F5F7)
val DarkTextSecondary = Color(0xFFBBC3CC)

val DarkActionCobalt = Color(0xFF8CA7FF)
val DarkActionCobaltSoft = Color(0xFF35446C)

val DarkLocalTeal = Color(0xFF64C7BB)
val DarkLocalTealSoft = Color(0xFF244944)

val DarkProcessAmber = Color(0xFFF0AD5A)
val DarkProcessAmberSoft = Color(0xFF5A4024)

val DarkDestructive = Color(0xFFFF8F86)
val DarkDestructiveSoft = Color(0xFF5B302D)

// Speaker palette: restrained categorical markers, never a rainbow UI.
val SpeakerBlue = Color(0xFF4667B1)
val SpeakerTeal = Color(0xFF3C756F)
val SpeakerPlum = Color(0xFF765A87)
val SpeakerOchre = Color(0xFF8A6A36)
