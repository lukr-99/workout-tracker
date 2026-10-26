package com.lukr99.workout.data.images

private const val ResourceName = "free_exercise_image_index.json"

/**
 * The text of the bundled free exercise image index JSON. Each platform supplies this file; the web
 * edition's is under web/. On Android it reads the classpath resource (`src/main/resources`), so the
 * app and JVM tests load the very same file.
 */
internal fun readFreeExerciseImageIndexJson(): String {
    val stream = FreeExerciseImageIndex::class.java.classLoader?.getResourceAsStream(ResourceName)
        ?: error("$ResourceName is missing from the app's resources.")
    return stream.bufferedReader().use { it.readText() }
}
