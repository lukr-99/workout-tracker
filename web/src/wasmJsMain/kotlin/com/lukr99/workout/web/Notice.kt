package com.lukr99.workout.web

/** One line of the [StorageBanner], with an optional button. */
internal data class Notice(
    val text: String,
    val action: String? = null,
    val onAction: () -> Unit = {},
)
