package com.lukr99.workout.data.backup

/** Instrumented-test seam; production never assigns this. */
internal object BackupWorkerTestHook {
    var run: (suspend () -> BackupRunSummary)? = null
}
