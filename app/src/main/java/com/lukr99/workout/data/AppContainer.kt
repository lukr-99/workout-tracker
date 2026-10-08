package com.lukr99.workout.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.lukr99.workout.data.backup.BackupRunner
import com.lukr99.workout.data.backup.BackupScheduler
import com.lukr99.workout.data.backup.SafBackupGateway
import com.lukr99.workout.data.health.AndroidHealthConnectGateway
import com.lukr99.workout.data.health.HealthConnectService
import com.lukr99.workout.data.images.ExerciseImageResolver
import com.lukr99.workout.data.images.ExercisePhotoStore
import com.lukr99.workout.data.images.FreeExerciseImageIndex
import com.lukr99.workout.data.importer.BundleImporter
import com.lukr99.workout.data.location.GpsWatcher
import com.lukr99.workout.data.location.RunSessionController
import com.lukr99.workout.data.map.OfflineTileCache
import com.lukr99.workout.data.music.SpotifyController
import com.lukr99.workout.data.music.StubSpotifyController
import com.lukr99.workout.data.routing.OsrmRoutingClient
import com.lukr99.workout.data.routing.RoutingClient
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.run.ShareCardRenderer
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.data.sync.WgerSyncService
import com.lukr99.workout.data.transfer.AndroidDocumentGateway
import com.lukr99.workout.data.transfer.DataStoreSettingsArchive
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.SettingsArchive
import com.lukr99.workout.data.transfer.UserDataEraser
import com.lukr99.workout.settings.DevicePrefs
import com.lukr99.workout.settings.OrientationStore
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.WhatsNewGate
import com.lukr99.workout.update.FileProviderInstallerLauncher
import com.lukr99.workout.update.GitHubReleaseSource
import com.lukr99.workout.update.HttpsUpdateHttp
import com.lukr99.workout.update.PackageSignatureCheck
import com.lukr99.workout.update.UpdateService
import com.lukr99.workout.update.VerifiedDownloader

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
    /** Run Mode persistence (v5 tables). Additive — lazily built so strength flows are unaffected. */
    val runRepository: RunRepository by lazy { RunRepository(db.runDao()) }

    /** Process-wide live-run brain + crash-buffer persistence (R1). One instance per process. */
    val runSessionController: RunSessionController by lazy {
        RunSessionController(context.applicationContext, runRepository)
    }

    /** Route snapping for the planner (R3) — keyless OSRM by default; the only routing IO boundary. */
    val routingClient: RoutingClient by lazy { OsrmRoutingClient() }

    /** Renders a run to a shareable image (R5 share-run card). */
    val shareCardRenderer: ShareCardRenderer by lazy { ShareCardRenderer(context) }

    /** Offline map-tile caching for a route/recent region (R5; closes the deferred R3 slice). */
    val offlineTileCache: OfflineTileCache by lazy { OfflineTileCache(context) }

    /** GPS accuracy before a run, for the Start a run card. */
    val gpsWatcher: GpsWatcher by lazy { GpsWatcher(context) }

    /**
     * Music control (R4), shared by the live run + lift screens. Ships as the Open-Spotify-only
     * [StubSpotifyController]; swap for an App Remote implementation to enable transport (see
     * docs/run-mode/handoff-R4.md).
     */
    val spotify: SpotifyController = StubSpotifyController
    val workoutData: WorkoutDataService by lazy { WorkoutDataService(repository) }
    val insights: WorkoutInsightsService by lazy { WorkoutInsightsService(repository) }
    val wgerSync: WgerSyncService by lazy { WgerSyncService(repository) }
    val exerciseImages: ExerciseImageResolver by lazy {
        ExerciseImageResolver(FreeExerciseImageIndex())
    }
    val exercisePhotos: ExercisePhotoStore by lazy { ExercisePhotoStore(context.applicationContext) }
    /** versionName of this build, written into every export so a backup says what made it. */
    private val appVersion: String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()
    private val settingsArchive: SettingsArchive by lazy { DataStoreSettingsArchive(settings) }
    val dataTransfer: DataTransferService by lazy {
        DataTransferService(
            repository = repository,
            runRepository = runRepository,
            photos = exercisePhotos,
            settings = settingsArchive,
            appVersion = appVersion,
        )
    }
    /** The updater: public GitHub Releases, exact asset names, SHA-256 and signer checks. */
    val updates: UpdateService by lazy {
        val http = HttpsUpdateHttp(userAgent = "Ember/${appVersion ?: "unknown"}")
        UpdateService(
            currentVersion = appVersion ?: "0",
            source = GitHubReleaseSource(owner = "lukr-99", repo = "workout-tracker", http = http),
            http = http,
            downloader = VerifiedDownloader(http, java.io.File(context.cacheDir, "updates")),
            signatureCheck = PackageSignatureCheck(context.applicationContext),
            launcher = FileProviderInstallerLauncher(context.applicationContext),
        )
    }
    val dataEraser: UserDataEraser by lazy {
        UserDataEraser(
            repository = repository,
            runRepository = runRepository,
            photos = exercisePhotos,
            settings = settingsArchive,
            stopAutomaticBackup = { backup.disable() },
            isRunRecording = { runSessionController.isRunning },
        )
    }
    val documents: AndroidDocumentGateway by lazy { AndroidDocumentGateway(context) }
    val settings: SettingsStore by lazy { SettingsStore(context) }

    /** This phone's own switches (Health Connect auto-send, update checks); not in backups. */
    val devicePrefs: DevicePrefs by lazy { DevicePrefs(context) }

    /** The "what's new" card on Home, shown once after an update that has a note. */
    val whatsNew: WhatsNewGate by lazy {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        WhatsNewGate(
            store = OrientationStore(context),
            current = PackageInfoCompat.getLongVersionCode(info).toInt(),
            freshInstall = info.firstInstallTime == info.lastUpdateTime,
        )
    }
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
