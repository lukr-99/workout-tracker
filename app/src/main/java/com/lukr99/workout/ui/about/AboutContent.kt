package com.lukr99.workout.ui.about

/**
 * The words on the About page that depend on the edition: what Ember keeps and where, and what it is
 * built from. Kept apart from the screen so tests can check them, and true to what the code does.
 */
object AboutContent {
    const val TAGLINE: String = "Strength training and runs, logged fast."

    /** One or two lines on what Ember is, for the hero. */
    fun summary(edition: AboutEdition): String = when (edition) {
        AboutEdition.Android ->
            "A workout log for the gym, with GPS runs, progress charts and records. It works offline and needs no account."
        AboutEdition.Web ->
            "A workout log for the gym, with progress charts and records. The same app as on Android, here in your browser, with no account."
    }

    /** The edition's name in the facts row. */
    fun editionName(edition: AboutEdition): String = when (edition) {
        AboutEdition.Android -> "Android app"
        AboutEdition.Web -> "Web edition"
    }

    /** "Version 2.11.0", or just "Version" when the build did not say. */
    fun versionLabel(version: String): String = version.trim().removePrefix("v").let { if (it.isEmpty()) "Version" else "Version $it" }

    /** Short points on what Ember keeps and where it goes, for the data and privacy card. */
    fun privacy(edition: AboutEdition): List<String> = when (edition) {
        AboutEdition.Android -> listOf(
            "Your workouts, runs, templates, photos and settings stay on this phone. There is no account and no ads.",
            "Ember goes online for app updates from GitHub, exercise pictures and catalog sync, map tiles and route snapping.",
            "Your training data leaves the phone only when you choose: a backup or export, Health Connect, or Claude, if you connect them.",
            "Delete all data in Settings, Your data, removes everything and starts fresh.",
        )
        AboutEdition.Web -> listOf(
            "Your workouts, templates, photos and settings are stored in this browser on this device. There is no account and no ads.",
            "Nothing is sent anywhere. The page only loads the app's own files.",
            "Clearing this site's data in the browser deletes everything, so export a backup now and then.",
        )
    }

    /** What Ember is made from, as this edition ships it. */
    fun credits(edition: AboutEdition): List<AboutCredit> = buildList {
        add(
            AboutCredit(
                AboutCreditKind.Source,
                "Ember",
                "Made by ${AuthorLinks.NAME}. The source is public on GitHub.",
                "PolyForm Noncommercial 1.0.0",
            ),
        )
        add(AboutCredit(AboutCreditKind.Type, "Typefaces", "Barlow and Barlow Condensed by the Barlow Project Authors.", "SIL Open Font License 1.1"))
        add(AboutCredit(AboutCreditKind.Icons, "Icons", "Material Icons by Google.", "Apache License 2.0"))
        add(
            AboutCredit(
                AboutCreditKind.Libraries,
                "Built with",
                when (edition) {
                    AboutEdition.Android -> "Kotlin, Jetpack Compose, Room, WorkManager, kotlinx libraries, OkHttp, Coil and Firebase Cloud Messaging."
                    AboutEdition.Web -> "Kotlin, Compose Multiplatform and kotlinx libraries, compiled to WebAssembly."
                },
                "Apache License 2.0",
            ),
        )
        if (edition == AboutEdition.Android) {
            add(
                AboutCredit(
                    AboutCreditKind.Pictures,
                    "Exercise pictures",
                    "From wger, with the author shown on each picture, and from free-exercise-db.",
                    "CC BY-SA 4.0 and The Unlicense",
                ),
            )
            add(
                AboutCredit(
                    AboutCreditKind.Maps,
                    "Maps and routes",
                    "MapLibre draws the map. Tiles from OpenFreeMap, routes snapped by OSRM. Map data by OpenStreetMap contributors.",
                    "BSD 2-Clause and ODbL",
                ),
            )
        }
    }
}
