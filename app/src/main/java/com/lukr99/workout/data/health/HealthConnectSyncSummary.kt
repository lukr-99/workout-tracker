package com.lukr99.workout.data.health

data class HealthConnectSyncSummary(
    val imported: Int = 0,
    val exported: Int = 0,
    val skipped: Int = 0,
    val unsupported: Int = 0,
) {
    operator fun plus(other: HealthConnectSyncSummary) = HealthConnectSyncSummary(
        imported = imported + other.imported,
        exported = exported + other.exported,
        skipped = skipped + other.skipped,
        unsupported = maxOf(unsupported, other.unsupported),
    )
}
