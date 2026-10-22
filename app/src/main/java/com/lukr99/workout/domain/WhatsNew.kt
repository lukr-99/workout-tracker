package com.lukr99.workout.domain

/**
 * What changed in each release, for the card on Home. Add a note with the release's `versionCode`
 * when you release (docs/RELEASING.md): three to five short lines a user would notice. The full
 * list stays in CHANGELOG.md.
 */
object WhatsNew {

    val notes: List<WhatsNewNote> = listOf(
        WhatsNewNote(
            versionCode = 13,
            title = "New in Ember",
            lines = listOf(
                "Pick a gym, Home or Outdoors for each workout. Set it up in Settings, Locations.",
                "Track an exercise per gym, so each gym keeps its own numbers.",
                "Gym notes, like \"seat 4\", show on an exercise at that gym.",
                "Compare gyms on the progress chart with the This gym / All gyms switch.",
                "Save a gym's position and Ember picks it when you start a workout there.",
            ),
        ),
        WhatsNewNote(
            versionCode = 12,
            title = "New in Ember",
            lines = listOf(
                "Tap a day in the week strip on Home to open its workout or run.",
                "New set tags: Finisher, Explosive, Slow down and Slow up.",
                "Share a workout as text, or as a new image that lists your exercises.",
                "Checking for updates works again.",
            ),
        ),
        WhatsNewNote(
            versionCode = 11,
            title = "New in Ember",
            lines = listOf(
                "Repeat a past workout, or switch to a template, from an empty workout.",
                "Exercises you have not reached fold up. Tap one to open it.",
                "Duplicate a set, and log effort as reps left in the tank.",
                "Drag to reorder a template, and see how long its plan takes.",
                "A new live run card, light maps, and GPS status before you start.",
            ),
        ),
        WhatsNewNote(
            versionCode = 10,
            title = "New in Ember",
            lines = listOf(
                "A new look: warmer colours, new fonts, and a new Home, Library, Runs and Progress.",
                "Settings is one page. Tap a chip at the top to jump to a section.",
                "Templates can plan sets, a rep range, rest and supersets for each exercise.",
                "Notes you can see while you lift, and how-to steps for each exercise.",
            ),
        ),
    )

    /**
     * The note to show, or null. [lastSeen] is the version whose note was last dismissed, or null
     * when none was. A fresh install shows nothing: there is nothing new to someone who just
     * arrived. Updating past several releases shows the newest note only.
     */
    fun due(lastSeen: Int?, current: Int, freshInstall: Boolean, all: List<WhatsNewNote> = notes): WhatsNewNote? {
        if (lastSeen == null && freshInstall) return null
        val after = lastSeen ?: 0
        return all.filter { it.versionCode in (after + 1)..current }.maxByOrNull { it.versionCode }
    }
}
