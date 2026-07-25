package com.lukr99.workout.data

import android.content.Context
import com.lukr99.workout.data.importer.BundleImporter

/**
 * Manual dependency graph (ServiceLocator, ring-set style — see 01-architecture.md "DI"):
 * builds `Db → Dao → Repository` once and hands the repository to the ViewModel layer. Kept
 * deliberately small; adopt Hilt only if the graph gets deep.
 */
class AppContainer(context: Context) {
    private val db: WorkoutDb = WorkoutDb.build(context)
    val repository: WorkoutRepository = WorkoutRepository(db.workoutDao())
    val bundleImporter: BundleImporter by lazy { BundleImporter(repository) }
}
