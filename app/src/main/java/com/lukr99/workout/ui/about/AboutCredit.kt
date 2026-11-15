package com.lukr99.workout.ui.about

/** One line of the About page's credits: what it is for, who made it, and its licence. */
data class AboutCredit(
    val kind: AboutCreditKind,
    val label: String,
    val credit: String,
    val licence: String,
)
