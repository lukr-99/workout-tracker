package com.lukr99.workout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lukr99.workout.R

/**
 * Barlow for reading, Barlow Condensed for titles and numbers, from the approved redesign. Both are
 * bundled (SIL Open Font License, `assets/licenses/barlow-OFL.txt`), so the app looks the same
 * offline and on every phone.
 */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

private val base = Typography()

/** Every Material style in Barlow, with the app's own sizes for the ones the screens use. */
val WorkoutTypography = Typography(
    displayLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp),
    displayMedium = base.displayMedium.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 31.sp),
    titleMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = base.titleSmall.copy(fontFamily = Barlow, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Barlow, fontSize = 15.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Barlow),
    labelLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

/**
 * Numbers in Barlow Condensed with tabular figures, so weights, reps and times line up in columns.
 * Callers set the size.
 */
val Numbers = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)
