package com.lukr99.workout.data.images

import com.lukr99.workout.web.WebResources

/**
 * The text of the bundled free exercise image index JSON. Each platform supplies this file. The web
 * edition reads the copy that preloadWebResources() fetched from beside the page.
 */
internal fun readFreeExerciseImageIndexJson(): String = WebResources.freeExerciseImageIndexJson
