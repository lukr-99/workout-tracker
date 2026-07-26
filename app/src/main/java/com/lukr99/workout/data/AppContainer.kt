package com.lukr99.workout.data

import android.content.Context
import com.lukr99.workout.data.backup.BackupRunner
import com.lukr99.workout.data.backup.BackupScheduler
import com.lukr99.workout.data.backup.SafBackupGateway
import com.lukr99.workout.data.health.AndroidHealthConnectGateway
import com.lukr99.workout.data.health.HealthConnectService
import com.lukr99.workout.data.images.ExerciseImageResolver
import com.lukr99.workout.data.images.ExercisePhotoStore
import com.lukr99.workout.data.images.FreeExerciseImageIndex
import com.lukr99.workout.data.importer.BundleImporter
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.data.sync.WgerSyncService
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.settings.SettingsStore

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
    val insights: WorkoutInsightsService by lazy { WorkoutInsightsService(repository) }
    val wgerSync: WgerSyncService by lazy { WgerSyncService(repository) }
    val exerciseImages: ExerciseImageResolver by lazy {
        ExerciseImageResolver(FreeExerciseImageIndex(context.assets))
    }
    val exercisePhotos: ExercisePhotoStore by lazy { ExercisePhotoStore(context.applicationContext) }
    val dataTransfer: DataTransferService by lazy { DataTransferService(repository) }
    val documents: AndroidDocumentGateway by lazy { AndroidDocumentGateway(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }
    val healthConnect: HealthConnectService by lazy {
        HealthConnectService(repository, AndroidHealthConnectGateway(context))
    }
    val backup: BackupScheduler by lazy {
        BackupScheduler(
            context,
            BackupRunner(
                exportJson = { dataTransfer.exportJson().text },
                gateway = SafBackupGateway(context),
            ),
        )
    }

    /** Compatibility endpoint for Phase 1 callers; new flows should use [dataTransfer]. */
    val bundleImporter: BundleImporter by lazy { BundleImporter(repository) }
}
