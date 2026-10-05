package com.lukr99.workout.update

/** Where releases are discovered. */
fun interface ReleaseSource {
    /** The latest published release. Throws on a network or format problem. */
    suspend fun latest(): PublishedRelease
}
