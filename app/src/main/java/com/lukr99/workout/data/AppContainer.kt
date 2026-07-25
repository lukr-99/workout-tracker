package com.lukr99.workout.data

import android.content.Context
import com.lukr99.workout.data.importer.BundleImporter
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.DataTransferService

/**
 * Manual dependency graph (ServiceLocator, ring-set style — see 01-architecture.md "DI"):
 * builds `Db → Dao → Repository` once and hands the repository to the ViewModel layer. Kept
 * deliberately small; adopt Hilt only if the graph gets deep.
 */
class AppContainer(context: Context) {
    private val db: WorkoutDb = WorkoutDb.build(context)
    val repository: WorkoutRepository = WorkoutRepository(
        db.workoutDao(),
        RoomTransactionRunner(db),
    )
    val workoutData: WorkoutDataService by lazy { WorkoutDataService(repository) }
    val dataTransfer: DataTransferService by lazy { DataTransferService(repository) }
    val documents: AndroidDocumentGateway by lazy { AndroidDocumentGateway(context) }

    /** Compatibility endpoint for Phase 1 callers; new flows should use [dataTransfer]. */
    val bundleImporter: BundleImporter by lazy { BundleImporter(repository) }
}
