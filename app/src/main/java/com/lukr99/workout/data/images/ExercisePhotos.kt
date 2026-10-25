package com.lukr99.workout.data.images

/**
 * What the Library ViewModel needs from photo storage: keep the bytes of a picked or captured
 * photo, and remove one. The returned string is the key stored on the exercise. Android implements
 * it with app-private files ([ExercisePhotoStore]); the web edition stores the bytes in the browser.
 */
interface ExercisePhotos {
    /** Saves [bytes] as the photo of [exerciseId], replacing the old one, and returns its key. */
    fun savePhoto(exerciseId: String, bytes: ByteArray): String

    /** Removes the photo with this key. A null or unknown key is ignored. */
    fun removePhoto(path: String?)
}
