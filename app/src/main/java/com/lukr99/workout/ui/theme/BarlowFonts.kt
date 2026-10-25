package com.lukr99.workout.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lukr99.workout.R

/*
 * The bundled Barlow font families. Each platform supplies this file (Android reads R.font; the
 * web edition's, under web/, loads the same TTFs).
 */

/** Barlow for reading, bundled under the SIL Open Font License (`assets/licenses/barlow-OFL.txt`). */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/** Barlow Condensed for titles and numbers. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)
