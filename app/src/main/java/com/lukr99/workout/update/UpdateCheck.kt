package com.lukr99.workout.update

/** The answer to "is there an update?". */
sealed interface UpdateCheck {
    data object UpToDate : UpdateCheck

    /** A debug build: it is a separate app, so a release APK would install a second copy. */
    data object DevelopmentBuild : UpdateCheck

    data class Available(val offer: UpdateOffer) : UpdateCheck

    /** A newer release exists but lacks the expected files; [reason] says which. */
    data class NotVerifiable(val version: String, val reason: String) : UpdateCheck
}
