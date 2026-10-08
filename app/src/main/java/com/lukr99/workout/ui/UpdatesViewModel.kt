package com.lukr99.workout.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.settings.DevicePrefs
import com.lukr99.workout.update.UpdateCheck
import com.lukr99.workout.update.UpdateCheckSchedule
import com.lukr99.workout.update.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Drives the Updates section: check, offer, verified download, then the system installer. */
class UpdatesViewModel(
    private val service: UpdateService,
    private val prefs: DevicePrefs? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UpdatesState(currentVersion = service.currentVersion))
    val state: StateFlow<UpdatesState> = mutableState.asStateFlow()

    /** Whether Ember looks for updates by itself on launch. */
    val autoCheck: StateFlow<Boolean> =
        (prefs?.updateAutoCheck ?: flowOf(false)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAutoCheck(on: Boolean) {
        viewModelScope.launch { prefs?.setUpdateAutoCheck(on) }
    }

    /**
     * The launch check: quiet unless a verified update is out, and at most once a day
     * ([UpdateCheckSchedule]). A found update becomes the [UpdatesState.offer] Home shows.
     */
    fun checkIfDue() {
        val prefs = prefs ?: return
        viewModelScope.launch {
            val at = now()
            if (!UpdateCheckSchedule.isDue(prefs.updateAutoCheck.first(), prefs.lastUpdateCheckUtc.first(), at)) return@launch
            prefs.markUpdateChecked(at)
            val result = runCatching { service.check() }.getOrNull()
            if (result is UpdateCheck.Available) mutableState.update { it.copy(offer = result.offer) }
        }
    }

    fun check() {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, progress = 0f, status = "Checking for updates…") }
        viewModelScope.launch {
            val status = runCatching { service.check() }.fold(
                onSuccess = { result ->
                    when (result) {
                        UpdateCheck.UpToDate -> "You're on the latest version."
                        UpdateCheck.DevelopmentBuild ->
                            "This is Ember dev, a development build. It does not update itself."
                        is UpdateCheck.Available -> {
                            mutableState.update { it.copy(offer = result.offer) }
                            "Update available: v${result.offer.version}"
                        }
                        is UpdateCheck.NotVerifiable ->
                            "v${result.version} is out but cannot be verified (${result.reason}) " +
                                "Download it from the releases page instead."
                    }
                },
                onFailure = { failure("Check failed", it) },
            )
            mutableState.update { it.copy(busy = false, status = status) }
        }
    }

    fun downloadAndInstall() {
        val offer = state.value.offer ?: return
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, progress = 0f, status = "Downloading v${offer.version}… 0%") }
        viewModelScope.launch {
            runCatching {
                service.download(offer) { fraction ->
                    mutableState.update {
                        it.copy(progress = fraction, status = "Downloading v${offer.version}… ${(fraction * 100).toInt()}%")
                    }
                }
            }.onSuccess { apk ->
                mutableState.update { it.copy(busy = false, offer = null, status = "Opening the installer…") }
                runCatching { service.install(apk) }
                    .onFailure { error -> mutableState.update { it.copy(status = failure("Couldn't open the installer", error)) } }
            }.onFailure { error ->
                mutableState.update { it.copy(busy = false, status = failure("Download failed", error)) }
            }
        }
    }

    fun dismissOffer() {
        if (!state.value.busy) mutableState.update { it.copy(offer = null) }
    }

    /** A failure for the status line, logged so it can be found later. */
    private fun failure(prefix: String, cause: Throwable): String {
        Log.w("Updates", prefix, cause)
        val detail = cause.message?.takeIf(String::isNotBlank) ?: cause::class.java.simpleName
        return "$prefix: $detail"
    }

    companion object {
        const val RELEASES_URL = "https://github.com/lukr-99/workout-tracker/releases"

        fun factory(service: UpdateService, prefs: DevicePrefs): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdatesViewModel(service, prefs) as T
        }
    }
}
