package com.lukr99.workout.data.sync

data class WgerSyncSummary(
    val fetched: Int,
    val mapped: Int,
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val pages: Int,
    val warnings: List<String>,
    val imagesBackfilled: Int = 0,
) {
    val changed: Int get() = added + updated
}
