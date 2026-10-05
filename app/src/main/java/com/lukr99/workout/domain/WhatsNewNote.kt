package com.lukr99.workout.domain

/** The short list Home shows once after an update to [versionCode]. */
data class WhatsNewNote(
    val versionCode: Int,
    val title: String,
    val lines: List<String>,
)
