package com.lukr99.workout.web

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.lukr99.workout.web.platform.BrowserResourceFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * The files the web edition has to fetch before its first screen. Android reads the same files
 * synchronously (`R.font`, classpath resources); a browser has to fetch them, so they are loaded once
 * by [preloadWebResources] and then stand where those resources stand on Android.
 */
internal object WebResources {
    private var loadedBarlow: FontFamily? = null
    private var loadedBarlowCondensed: FontFamily? = null
    private var loadedFreeExerciseImageIndex: String? = null

    /** Barlow, the reading typeface. */
    val barlow: FontFamily get() = checkNotNull(loadedBarlow) { "Fonts are read before preloadWebResources()" }

    /** Barlow Condensed, for titles and numbers. */
    val barlowCondensed: FontFamily
        get() = checkNotNull(loadedBarlowCondensed) { "Fonts are read before preloadWebResources()" }

    /** The text of the free exercise image index; "{}" when it could not be fetched. */
    val freeExerciseImageIndexJson: String get() = loadedFreeExerciseImageIndex ?: "{}"

    suspend fun load() = coroutineScope {
        val fonts = async { loadFonts() }
        val index = async { BrowserResourceFetcher.text(FREE_EXERCISE_IMAGE_INDEX_PATH) }
        fonts.await()
        // The index only feeds exercise pictures, which the web edition does not draw yet, so a
        // missing file is not worth stopping the app for.
        loadedFreeExerciseImageIndex = index.await()
    }

    /**
     * A page with no fonts would draw no text at all (a canvas has no system fonts to fall back on),
     * so a font that cannot be fetched stops the start-up rather than leaving a blank app.
     */
    private suspend fun loadFonts() = coroutineScope {
        val barlowFiles = listOf(
            "barlow_regular" to FontWeight.Normal,
            "barlow_medium" to FontWeight.Medium,
            "barlow_semibold" to FontWeight.SemiBold,
            "barlow_bold" to FontWeight.Bold,
        )
        val condensedFiles = listOf(
            "barlow_condensed_medium" to FontWeight.Medium,
            "barlow_condensed_semibold" to FontWeight.SemiBold,
            "barlow_condensed_bold" to FontWeight.Bold,
        )
        val fetched = (barlowFiles + condensedFiles).map { (file, weight) ->
            async {
                val bytes = checkNotNull(BrowserResourceFetcher.bytes("fonts/$file.ttf")) { "Font $file could not be fetched" }
                file to Font(identity = file, data = bytes, weight = weight)
            }
        }.awaitAll().toMap()
        loadedBarlow = FontFamily(barlowFiles.map { fetched.getValue(it.first) })
        loadedBarlowCondensed = FontFamily(condensedFiles.map { fetched.getValue(it.first) })
    }

    private const val FREE_EXERCISE_IMAGE_INDEX_PATH = "free_exercise_image_index.json"
}

/**
 * Fetches the fonts and the exercise image index. `main` must finish this before the first frame:
 * the typefaces and the image index are read from what it loaded.
 */
suspend fun preloadWebResources() = WebResources.load()
