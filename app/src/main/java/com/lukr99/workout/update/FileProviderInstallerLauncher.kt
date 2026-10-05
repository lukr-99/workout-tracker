package com.lukr99.workout.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Opens the system package installer for an APK in the cache `updates/` folder. */
class FileProviderInstallerLauncher(
    private val context: Context,
    private val authority: String = "${context.packageName}.files",
) : InstallerLauncher {
    override fun launch(apk: File) {
        val uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
