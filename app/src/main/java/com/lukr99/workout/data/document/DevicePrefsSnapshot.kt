package com.lukr99.workout.data.document

import kotlinx.serialization.Serializable

/** The stored form of [com.lukr99.workout.settings.DevicePrefs]. Never part of a backup. */
@Serializable
data class DevicePrefsSnapshot(
    val healthAutoSend: Boolean = true,
    val updateAutoCheck: Boolean = true,
    val lastUpdateCheckUtc: Long? = null,
    val locationSetupDismissed: Boolean = false,
    val positionOfferAnswered: Set<String> = emptySet(),
)
