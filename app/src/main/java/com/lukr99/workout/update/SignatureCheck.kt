package com.lukr99.workout.update

import java.io.File

/** Whether a downloaded APK is signed by the same key as the installed app. */
fun interface SignatureCheck {
    fun sameSigner(apk: File): Boolean
}
