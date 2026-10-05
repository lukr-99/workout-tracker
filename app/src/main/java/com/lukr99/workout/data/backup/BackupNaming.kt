package com.lukr99.workout.data.backup

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal object BackupNaming {
    private val formatter = DateTimeFormatter
        .ofPattern("'workout-backup-'yyyyMMdd-HHmmss-SSS'.json'")
        .withZone(ZoneOffset.UTC)

    fun fileName(utcMillis: Long): String = formatter.format(Instant.ofEpochMilli(utcMillis))

    fun isManagedBackup(name: String): Boolean =
        name.startsWith("workout-backup-") && name.endsWith(".json", ignoreCase = true)
}
