package com.lukr99.workout.data.sync

data class WgerSyncOptions(
    val language: Int = 2,
    val limit: Int = 2_000,
    val pageSize: Int = 50,
    val offset: Int = 0,
    val maxPages: Int = 40,
) {
    internal fun validate() {
        require(language > 0) { "language must be positive." }
        require(limit > 0) { "limit must be positive." }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100." }
        require(offset >= 0) { "offset must not be negative." }
        require(maxPages > 0) { "maxPages must be positive." }
    }
}
