package com.lukr99.workout.ui.settings

import android.content.Intent
import android.net.Uri
import com.lukr99.workout.data.backup.BackupResult
import com.lukr99.workout.data.health.HealthConnectAvailability
import java.text.DateFormat
import java.util.Date

/* Text and small actions for the Settings rows: status lines that are easier to read than raw states. */

internal fun healthAvailabilityLabel(
    availability: HealthConnectAvailability?,
    connected: Boolean,
): String = when (availability) {
    HealthConnectAvailability.Available -> if (connected) "Connected" else "Available"
    HealthConnectAvailability.ProviderUpdateRequired -> "Provider update required"
    HealthConnectAvailability.Unavailable -> "Unavailable on this device"
    null -> "Checking availability…"
}

internal fun healthAvailabilityDetail(
    availability: HealthConnectAvailability?,
    connected: Boolean,
): String = when (availability) {
    HealthConnectAvailability.Available ->
        if (connected) "Exercise and weight permissions granted" else "Connect to import or export workouts"
    HealthConnectAvailability.ProviderUpdateRequired -> "Install or update Health Connect to continue"
    HealthConnectAvailability.Unavailable -> "Health Connect is not supported by this device"
    null -> "Reading provider status"
}

internal fun openHealthConnectListing(context: android.content.Context) {
    val packageName = "com.google.android.apps.healthdata"
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    val browser = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
    )
    runCatching { context.startActivity(market) }
        .recoverCatching { context.startActivity(browser) }
}

internal fun backupFolderLabel(treeUri: String?): String {
    if (treeUri.isNullOrBlank()) return "Backup folder selected"
    return Uri.decode(treeUri.substringAfterLast('/')).ifBlank { "Backup folder selected" }
}

internal fun backupStatus(
    lastRunUtcMillis: Long?,
    result: BackupResult,
    message: String?,
    uiError: String?,
): String {
    uiError?.let { return it }
    val whenRun = lastRunUtcMillis?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    } ?: return "No automatic backup has run yet"
    val resultLabel = when (result) {
        BackupResult.Success -> "Succeeded"
        BackupResult.Failed -> "Failed"
        BackupResult.Disabled -> "Disabled"
        BackupResult.NeverRun -> "Not run"
    }
    return listOfNotNull("$resultLabel · $whenRun", message?.takeIf(String::isNotBlank))
        .joinToString(" · ")
}
