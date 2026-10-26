package com.lukr99.workout.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.lukr99.workout.web.WebResources

/*
 * The bundled Barlow font families. Each platform supplies this file; the web edition's loads the
 * same TTFs Android keeps in res/font, fetched by preloadWebResources() before the first frame.
 */

/** Barlow for reading, bundled under the SIL Open Font License (`assets/licenses/barlow-OFL.txt`). */
val Barlow: FontFamily get() = WebResources.barlow

/** Barlow Condensed for titles and numbers. */
val BarlowCondensed: FontFamily get() = WebResources.barlowCondensed
