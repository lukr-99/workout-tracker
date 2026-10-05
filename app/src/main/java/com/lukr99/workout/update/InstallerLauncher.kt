package com.lukr99.workout.update

import java.io.File

/** Hands a verified APK to the system package installer, which stays the final authority. */
fun interface InstallerLauncher {
    fun launch(apk: File)
}
