package com.lukr99.workout.ui.about

/**
 * Who made Ember and where to reach them, shown on the About page. Plain constants in one place, so
 * the name and the links cannot drift apart. The links open in the browser; the app sends nothing.
 */
object AuthorLinks {
    const val NAME: String = "Lukáš Krejčí"

    const val INSTAGRAM_HANDLE: String = "lukas9krejci"
    const val INSTAGRAM_URL: String = "https://www.instagram.com/$INSTAGRAM_HANDLE/"

    const val GITHUB_HANDLE: String = "lukr-99"
    const val GITHUB_URL: String = "https://github.com/$GITHUB_HANDLE"

    /** Ember's own public repository. */
    const val REPOSITORY_URL: String = "$GITHUB_URL/workout-tracker"

    /** Where bugs and ideas go. */
    const val ISSUES_URL: String = "$REPOSITORY_URL/issues"

    /** The author's initials for the monogram, like "LK". */
    fun initials(name: String = NAME): String =
        name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}
