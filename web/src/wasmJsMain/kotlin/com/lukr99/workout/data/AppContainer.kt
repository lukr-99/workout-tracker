package com.lukr99.workout.data

import com.lukr99.workout.data.images.ExerciseImageResolver
import com.lukr99.workout.data.images.ExercisePhotos
import com.lukr99.workout.data.images.FreeExerciseImageIndex
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.UserDataEraser
import com.lukr99.workout.settings.DevicePrefs
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.WhatsNewGate
import com.lukr99.workout.web.RunsHiddenFromScreens
import com.lukr99.workout.web.WEB_VERSION_NAME
import com.lukr99.workout.web.storage.WebStorage

/**
 * The web edition's dependency graph, with the members `App` reads. Each platform supplies this
 * file; Android's builds Room, DataStore and the Android services. Here everything stands on the
 * browser document store in [storage].
 */
class AppContainer(storage: WebStorage) {
    val repository: WorkoutRepository = storage.repository
    /** What the screens read: no runs, since the web edition has no Run Mode. Backups use the full store. */
    val runRepository: RunRepository = RunsHiddenFromScreens(storage.runRepository)
    val settings: SettingsStore = storage.settings
    val devicePrefs: DevicePrefs = storage.devicePrefs

    /** "What's new" notes describe Android releases, so the web edition never shows them. */
    val whatsNew: WhatsNewGate = WhatsNewGate(storage.seenVersions, current = 0, freshInstall = true)

    val workoutData: WorkoutDataService = WorkoutDataService(repository)
    val insights: WorkoutInsightsService = WorkoutInsightsService(repository)

    val exercisePhotos: ExercisePhotos = storage.photos

    /** A browser page cannot follow the phone's position in the background, so no gym suggestions. */
    val locationSuggester: PlaceSuggester = NoPlaceSuggester

    val exerciseImages: ExerciseImageResolver = ExerciseImageResolver(FreeExerciseImageIndex())

    val dataTransfer: DataTransferService = DataTransferService(
        repository = repository,
        runRepository = storage.runRepository,
        photos = storage.photos,
        settings = storage.settingsArchive,
        appVersion = WEB_VERSION_NAME,
    )

    val dataEraser: UserDataEraser = UserDataEraser(
        repository = repository,
        runRepository = storage.runRepository,
        photos = storage.photos,
        settings = storage.settingsArchive,
        stopAutomaticBackup = {},
        isRunRecording = { false },
    )
}
