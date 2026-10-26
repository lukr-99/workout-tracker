package com.lukr99.workout.data
import com.lukr99.workout.data.images.ExerciseImageResolver
import com.lukr99.workout.data.images.ExercisePhotos

import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.services.WorkoutDataService
import com.lukr99.workout.data.services.WorkoutInsightsService
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.UserDataEraser
import com.lukr99.workout.settings.DevicePrefs
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.WhatsNewGate
class AppContainer {
    val repository: WorkoutRepository = TODO()
    val runRepository: RunRepository = TODO()
    val whatsNew: WhatsNewGate = TODO()
    val workoutData: WorkoutDataService = TODO()
    val insights: WorkoutInsightsService = TODO()
    val settings: SettingsStore = TODO()
    val exercisePhotos: ExercisePhotos = TODO()
    val locationSuggester: PlaceSuggester = TODO()
    val devicePrefs: DevicePrefs = TODO()
    val dataTransfer: DataTransferService = TODO()
    val dataEraser: UserDataEraser = TODO()
    val exerciseImages: ExerciseImageResolver = TODO()
}
