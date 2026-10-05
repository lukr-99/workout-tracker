package com.lukr99.workout.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Compares the downloaded APK's package name and signing certificates with the installed app's.
 * Android checks this again at install time; checking first turns a wrong file into a clear message
 * instead of the installer's generic "App not installed".
 */
class PackageSignatureCheck(private val context: Context) : SignatureCheck {

    override fun sameSigner(apk: File): Boolean {
        val pm = context.packageManager
        val installed = digests(pm.getPackageInfo(context.packageName, FLAGS))
        @Suppress("DEPRECATION")
        val archive = pm.getPackageArchiveInfo(apk.path, FLAGS) ?: return false
        return archive.packageName == context.packageName && installed.isNotEmpty() && digests(archive) == installed
    }

    private fun digests(info: PackageInfo): Set<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { signing ->
                if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        } ?: return emptySet()
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    private companion object {
        @Suppress("DEPRECATION")
        val FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
    }
}
