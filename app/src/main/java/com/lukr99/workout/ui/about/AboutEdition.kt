package com.lukr99.workout.ui.about

/** Which build of Ember is running. The About page states where data lives and what it uses by this. */
enum class AboutEdition {
    /** The Android app: Room on the phone, with Run Mode and the online extras. */
    Android,

    /** The web edition: the same app in the browser, with its data in the browser. */
    Web,
}
