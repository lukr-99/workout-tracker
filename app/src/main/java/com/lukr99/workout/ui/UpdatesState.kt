package com.lukr99.workout.ui

import com.lukr99.workout.update.UpdateOffer

/** What the Updates section shows. [status] replaces the version line while it is not blank. */
data class UpdatesState(
    val currentVersion: String,
    val busy: Boolean = false,
    /** 0 while unknown; the download's share done otherwise. */
    val progress: Float = 0f,
    val status: String = "",
    /** A verified newer release waiting for the owner's "Download and install". */
    val offer: UpdateOffer? = null,
)
