package com.lukr99.workout.ui.android

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.backup.BackupScheduler
import com.lukr99.workout.data.backup.BackupState
import com.lukr99.workout.data.health.HealthConnectAvailability
import com.lukr99.workout.data.health.HealthConnectService
import com.lukr99.workout.data.health.HealthConnectSyncSummary
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.sync.WgerSyncOptions
import com.lukr99.workout.data.sync.WgerSyncService
import com.lukr99.workout.data.sync.WgerSyncSummary
import com.lukr99.workout.settings.DevicePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Android-only Settings parts: Health Connect, automatic backup and the wger catalog. The web
 * edition has none of these, so this file is not compiled there.
 */
class AndroidSettingsViewModel(
    private val wgerSync: WgerSyncService,
    private val healthConnect: HealthConnectService,
    private val backup: BackupScheduler,
    private val devicePrefs: DevicePrefs,
    private val runs: RunRepository,
) : ViewModel() {

    /** Whether finished workouts and runs go to Health Connect by themselves. */
    val healthAutoSend: StateFlow<Boolean> =
        devicePrefs.healthAutoSend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHealthAutoSend(on: Boolean) = viewModelScope.launch { devicePrefs.setHealthAutoSend(on) }.let { }

    private val catalogSyncState = MutableStateFlow<CatalogSyncState>(CatalogSyncState.Idle)
    val catalogSync: StateFlow<CatalogSyncState> = catalogSyncState.asStateFlow()

    private val healthConnectState = MutableStateFlow(HealthConnectUiState())
    val healthConnectUi: StateFlow<HealthConnectUiState> = healthConnectState.asStateFlow()
    val healthConnectPermissions: Set<String> get() = healthConnect.requiredPermissions

    val backupState: StateFlow<BackupState> = backup.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BackupState(),
    )
    private val backupOptionsState = MutableStateFlow(BackupOptions())
    val backupOptions: StateFlow<BackupOptions> = backupOptionsState.asStateFlow()
    private val backupBusyState = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = backupBusyState.asStateFlow()
    private val backupErrorState = MutableStateFlow<String?>(null)
    val backupError: StateFlow<String?> = backupErrorState.asStateFlow()

    init {
        refreshHealthConnect()
    }

    /** Pull the wger exercise catalog and merge it (network work runs on IO). */
    fun syncCatalog() {
        if (catalogSyncState.value is CatalogSyncState.Running) return
        catalogSyncState.value = CatalogSyncState.Running
        viewModelScope.launch {
            catalogSyncState.value = try {
                val summary = withContext(Dispatchers.IO) { wgerSync.sync(WgerSyncOptions()) }
                CatalogSyncState.Done(summary)
            } catch (t: Throwable) {
                CatalogSyncState.Failed(t.message ?: "Sync failed")
            }
        }
    }

    fun dismissCatalogSync() {
        catalogSyncState.value = CatalogSyncState.Idle
    }

    fun refreshHealthConnect() {
        if (healthConnectState.value.refreshing) return
        healthConnectState.update { it.copy(refreshing = true, error = null) }
        viewModelScope.launch {
            healthConnectState.value = try {
                val availability = withContext(Dispatchers.IO) { healthConnect.availability() }
                val connected = availability == HealthConnectAvailability.Available &&
                    withContext(Dispatchers.IO) { healthConnect.hasPermissions() }
                healthConnectState.value.copy(
                    refreshing = false,
                    availability = availability,
                    connected = connected,
                    error = null,
                )
            } catch (t: Throwable) {
                healthConnectState.value.copy(
                    refreshing = false,
                    connected = false,
                    error = t.message ?: "Could not check Health Connect",
                )
            }
        }
    }

    fun onHealthPermissionsResult(granted: Set<String>) {
        healthConnectState.update {
            it.copy(connected = granted.containsAll(healthConnect.requiredPermissions), error = null)
        }
        refreshHealthConnect()
    }

    fun importFromHealthConnect() = runHealthSync(HealthSyncOperation.Import) {
        healthConnect.importSessions()
    }

    fun exportToHealthConnect() = runHealthSync(HealthSyncOperation.Export) {
        healthConnect.exportCompletedSessions() + healthConnect.exportRuns(runs.getRuns())
    }

    fun consumeHealthSummary() {
        healthConnectState.update { it.copy(summary = null) }
    }

    private fun runHealthSync(
        operation: HealthSyncOperation,
        action: suspend () -> HealthConnectSyncSummary,
    ) {
        if (healthConnectState.value.operation != null) return
        healthConnectState.update { it.copy(operation = operation, summary = null, error = null) }
        viewModelScope.launch {
            healthConnectState.value = try {
                val summary = withContext(Dispatchers.IO) { action() }
                healthConnectState.value.copy(operation = null, summary = summary)
            } catch (t: Throwable) {
                healthConnectState.value.copy(
                    operation = null,
                    error = t.message ?: "Health Connect sync failed",
                )
            }
        }
    }

    fun setBackupInterval(hours: Long) {
        val current = backupState.value
        backupOptionsState.update {
            it.copy(
                intervalHours = hours,
                retentionCount = if (current.enabled) current.retentionCount else it.retentionCount,
            )
        }
        rescheduleEnabledBackup()
    }

    fun setBackupRetention(count: Int) {
        val current = backupState.value
        backupOptionsState.update {
            it.copy(
                intervalHours = if (current.enabled) current.intervalHours else it.intervalHours,
                retentionCount = count,
            )
        }
        rescheduleEnabledBackup()
    }

    fun enableBackup(treeUri: Uri) {
        updateBackup {
            val options = backupOptionsState.value
            backup.enable(treeUri, options.intervalHours, options.retentionCount)
        }
    }

    fun disableBackup() = updateBackup { backup.disable() }

    private fun rescheduleEnabledBackup() {
        val current = backupState.value
        val uri = current.treeUri?.takeIf { current.enabled } ?: return
        enableBackup(Uri.parse(uri))
    }

    private fun updateBackup(action: suspend () -> Unit) {
        if (backupBusyState.value) return
        backupBusyState.value = true
        backupErrorState.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
            } catch (t: Throwable) {
                backupErrorState.value = t.message ?: "Could not update automatic backup"
            } finally {
                backupBusyState.value = false
            }
        }
    }

    sealed interface CatalogSyncState {
        data object Idle : CatalogSyncState
        data object Running : CatalogSyncState
        data class Done(val summary: WgerSyncSummary) : CatalogSyncState
        data class Failed(val message: String) : CatalogSyncState
    }

    data class HealthConnectUiState(
        val refreshing: Boolean = false,
        val availability: HealthConnectAvailability? = null,
        val connected: Boolean = false,
        val operation: HealthSyncOperation? = null,
        val summary: HealthConnectSyncSummary? = null,
        val error: String? = null,
    )

    enum class HealthSyncOperation { Import, Export }

    data class BackupOptions(
        val intervalHours: Long = 24,
        val retentionCount: Int = 7,
    )

    companion object {
        fun factory(
            wgerSync: WgerSyncService,
            healthConnect: HealthConnectService,
            backup: BackupScheduler,
            devicePrefs: DevicePrefs,
            runs: RunRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AndroidSettingsViewModel(wgerSync, healthConnect, backup, devicePrefs, runs) }
            }
    }
}
