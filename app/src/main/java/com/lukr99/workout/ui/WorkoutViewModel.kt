package com.lukr99.workout.ui

import androidx.lifecycle.ViewModel
import com.lukr99.workout.data.WorkoutRepository

/**
 * The screens' single entry point to state and actions (MVVM, unidirectional data flow).
 * Screens are `fun XScreen(vm: WorkoutViewModel)` and read `StateFlow`s exposed here.
 *
 * Phase 0: an empty shell holding the (stub) repository. Real state — catalog, templates, the live
 * session, history, analytics — is added per feature phase. Split into per-area ViewModels if the
 * surface grows (see 01-architecture.md).
 */
class WorkoutViewModel : ViewModel() {
    // Stub repository — no-arg so the default `by viewModels()` factory can construct this VM.
    // Phase 1 swaps in a real repo via a ViewModelProvider.Factory / AndroidViewModel.
    @Suppress("unused")
    private val repo = WorkoutRepository()
}
