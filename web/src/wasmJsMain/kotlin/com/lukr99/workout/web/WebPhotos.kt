package com.lukr99.workout.web

import com.lukr99.workout.data.document.DocumentPhotoArchive

/**
 * Where the drawing code finds the exercise photos. Android reads a photo from a file path; the
 * web edition keeps the photos in memory, so the image platform files read them through here.
 * `main` sets it before the first frame.
 */
internal object WebPhotos {
    var archive: DocumentPhotoArchive? = null

    /** The image model for the photo stored under [key], or null when there is none. */
    fun model(key: String): WebPhotoModel? = archive?.bytes(key)?.let { WebPhotoModel(key, it) }
}
