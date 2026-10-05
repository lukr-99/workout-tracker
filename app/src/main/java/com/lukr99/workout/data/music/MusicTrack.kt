package com.lukr99.workout.data.music

/** The currently-playing track, when a music remote is connected. */
data class MusicTrack(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
)
