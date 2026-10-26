package com.lukr99.workout.data.document

/** Stored data that could not be read and was set aside under [key] when the app opened. */
data class SetAside(val key: String, val reason: String)
