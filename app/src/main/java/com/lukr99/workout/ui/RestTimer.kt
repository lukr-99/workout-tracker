package com.lukr99.workout.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Counts the rest between sets down once a second in [scope]. Starting again restarts it. */
class RestTimer(private val scope: CoroutineScope) {

    private val stateFlow = MutableStateFlow(RestState())
    val state: StateFlow<RestState> = stateFlow.asStateFlow()

    private var job: Job? = null

    fun start(seconds: Int) {
        job?.cancel()
        stateFlow.value = RestState(running = true, remaining = seconds, total = seconds)
        job = scope.launch {
            while (stateFlow.value.running && stateFlow.value.remaining > 0) {
                delay(1_000)
                stateFlow.update { it.copy(remaining = (it.remaining - 1).coerceAtLeast(0)) }
            }
            if (stateFlow.value.remaining <= 0) stateFlow.update { it.copy(running = false) }
        }
    }

    fun add(seconds: Int) = stateFlow.update {
        it.copy(remaining = (it.remaining + seconds).coerceAtLeast(0), total = maxOf(it.total, it.remaining + seconds))
    }

    fun skip() {
        job?.cancel()
        stateFlow.value = RestState()
    }
}
