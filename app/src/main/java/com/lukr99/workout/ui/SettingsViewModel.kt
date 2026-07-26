package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.data.AppContainer
import com.lukr99.workout.data.sync.WgerSyncOptions
import com.lukr99.workout.data.sync.WgerSyncService
import com.lukr99.workout.data.sync.WgerSyncSummary
import com.lukr99.workout.settings.AppSettings
import com.lukr99.workout.settings.SettingsStore
import com.lukr99.workout.settings.ThemeMode
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Preferences: theme override, display units, default rest — and the wger catalog sync action. */
class SettingsViewModel(
    private val store: SettingsStore,
    private val wgerSync: WgerSyncService,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val catalogSyncState = MutableStateFlow<CatalogSyncState>(CatalogSyncState.Idle)
    val catalogSync: StateFlow<CatalogSyncState> = catalogSyncState.asStateFlow()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }.let { }
    fun setUnits(units: UnitSystem) = viewModelScope.launch { store.setUnits(units) }.let { }
    fun setDefaultRest(seconds: Int) = viewModelScope.launch { store.setDefaultRestSeconds(seconds) }.let { }

    /** Pull the wger exercise catalog and merge it (Phase 3.5 `wgerSync.sync`, network on IO). */
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

    fun dismissCatalogSync() { catalogSyncState.value = CatalogSyncState.Idle }

    sealed interface CatalogSyncState {
        data object Idle : CatalogSyncState
        data object Running : CatalogSyncState
        data class Done(val summary: WgerSyncSummary) : CatalogSyncState
        data class Failed(val message: String) : CatalogSyncState
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container.settings, container.wgerSync) as T
        }
    }
}
