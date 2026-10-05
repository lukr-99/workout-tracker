package com.lukr99.workout.update

/** A newer release that can be verified: its version, notes, the APK and its checksum file. */
data class UpdateOffer(
    val version: String,
    val notes: String,
    val apk: ReleaseAsset,
    val checksum: ReleaseAsset,
)
