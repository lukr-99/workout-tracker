package com.lukr99.workout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale from docs/rework/02-design-system.md.
 *
 * Family is the system default (Roboto) for now; numbers are the hero, so a [Numbers] style with
 * tabular figures is provided for set rows, charts, and stat tiles where columns must align.
 * Scale (sp): Display 34 · Title 22 · Section 17 (semibold) · Body 15 · Label 13 · Caption 11.5.
 */
val WorkoutTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 15.sp),
)

/**
 * Tabular-figures number style. Use for weights/reps/volume so digit columns line up.
 * (Compose maps this to the OpenType `tnum` feature via monospaced digits.)
 */
val Numbers = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)
