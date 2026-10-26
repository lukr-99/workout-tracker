package com.lukr99.workout.web.storage

import com.lukr99.workout.data.TransactionRunner
import com.lukr99.workout.data.document.DocumentDatabase
import com.lukr99.workout.data.document.DocumentDevicePrefs
import com.lukr99.workout.data.document.DocumentRunRepository
import com.lukr99.workout.data.document.DocumentSeenVersionStore
import com.lukr99.workout.data.document.DocumentSettingsStore
import com.lukr99.workout.data.document.DocumentStore
import com.lukr99.workout.data.document.DocumentTransactionRunner
import com.lukr99.workout.data.document.DocumentWorkoutRepository
import com.lukr99.workout.data.document.DroppedPhotoArchive
import com.lukr99.workout.data.document.StorageStatus
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.transfer.DataStoreSettingsArchive
import com.lukr99.workout.data.transfer.PhotoArchive
import com.lukr99.workout.data.transfer.SettingsArchive
import com.lukr99.workout.settings.DevicePrefs
import com.lukr99.workout.settings.SeenVersionStore
import com.lukr99.workout.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The web edition's storage, ready to hand to the app graph: one [DocumentDatabase] in IndexedDB
 * and every store the shared code needs on top of it. [start] opens the data, listens for other
 * tabs and asks the browser to keep the data.
 */
class WebStorage(
    private val scope: CoroutineScope,
    store: DocumentStore = IndexedDbDocumentStore(),
) {
    val database = DocumentDatabase(store)
    val repository: WorkoutRepository = DocumentWorkoutRepository(database)
    val runRepository: RunRepository = DocumentRunRepository(database)
    val settings: SettingsStore = DocumentSettingsStore(database)
    val settingsArchive: SettingsArchive = DataStoreSettingsArchive(settings)
    val devicePrefs: DevicePrefs = DocumentDevicePrefs(database)
    val seenVersions: SeenVersionStore = DocumentSeenVersionStore(database)
    val transactions: TransactionRunner = DocumentTransactionRunner(database)
    val photos: PhotoArchive = DroppedPhotoArchive

    /** Saved, failed, memory only, data set aside: for the banner. */
    val status: StateFlow<StorageStatus> = database.status

    private val persistentFlow = MutableStateFlow<Boolean?>(null)

    /** Whether the browser agreed to keep the data under storage pressure; null until it answers. */
    val persistent: StateFlow<Boolean?> = persistentFlow.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        onDataChanged { scope.launch { database.reload() } }
        onPageVisible { scope.launch { database.reload() } }
        scope.launch {
            database.open()
            val granted: Boolean = try {
                requestPersist().await<JsBoolean>().toBoolean()
            } catch (failure: Throwable) {
                false
            }
            persistentFlow.value = granted
        }
    }
}
