package com.lukr99.workout.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lukr99.workout.update.UpdateCheck
import com.lukr99.workout.update.UpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Drives the Updates section: check, offer, verified download, then the system installer. */
class UpdatesViewModel(private val service: UpdateService) : ViewModel() {
    private val mutableState = MutableStateFlow(UpdatesState(currentVersion = service.currentVersion))
    val state: StateFlow<UpdatesState> = mutableState.asStateFlow()

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

        fun factory(service: UpdateService): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = UpdatesViewModel(service) as T
        }
    }
}
