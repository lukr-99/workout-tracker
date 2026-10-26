package com.lukr99.workout.ui.components

/** A connected music player: whether it plays, and the three transport buttons. */
class MusicTransport(
    val isPlaying: Boolean,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
)
