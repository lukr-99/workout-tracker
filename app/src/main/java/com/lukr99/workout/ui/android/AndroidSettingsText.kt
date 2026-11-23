package com.lukr99.workout.ui.android

import com.lukr99.workout.ui.i18n.DeviceSettingsText
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
    HealthConnectAvailability.Available -> if (connected) DeviceSettingsText.healthConnected.now() else DeviceSettingsText.healthAvailable.now()
    HealthConnectAvailability.ProviderUpdateRequired -> DeviceSettingsText.healthUpdateRequired.now()
    HealthConnectAvailability.Unavailable -> DeviceSettingsText.healthUnavailable.now()
    null -> DeviceSettingsText.healthChecking.now()
}

internal fun healthAvailabilityDetail(
    availability: HealthConnectAvailability?,
    connected: Boolean,
): String = when (availability) {
    HealthConnectAvailability.Available ->
        if (connected) DeviceSettingsText.healthPermissionsGranted.now() else DeviceSettingsText.healthConnectToImport.now()
    HealthConnectAvailability.ProviderUpdateRequired -> DeviceSettingsText.healthInstallOrUpdate.now()
    HealthConnectAvailability.Unavailable -> DeviceSettingsText.healthNotSupported.now()
    null -> DeviceSettingsText.healthReadingStatus.now()
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
    val selected = DeviceSettingsText.backupFolderSelected.now()
    if (treeUri.isNullOrBlank()) return selected
    return Uri.decode(treeUri.substringAfterLast('/')).ifBlank { selected }
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
    } ?: return DeviceSettingsText.backupNeverRun.now()
    val resultLabel = when (result) {
        BackupResult.Success -> DeviceSettingsText.backupSucceeded.now()
        BackupResult.Failed -> DeviceSettingsText.backupFailed.now()
        BackupResult.Disabled -> DeviceSettingsText.backupDisabled.now()
        BackupResult.NeverRun -> DeviceSettingsText.backupNotRun.now()
    }
    return listOfNotNull("$resultLabel · $whenRun", message?.takeIf(String::isNotBlank))
        .joinToString(" · ")
}
