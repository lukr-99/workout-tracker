package com.lukr99.workout.web

import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.domain.run.Run
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The run store as the web edition's screens see it: no runs, because the web edition has no Run
 * Mode to open them in. Everything else goes to [store], so runs from an imported Android backup stay
 * in storage and in every backup made here.
 */
internal class RunsHiddenFromScreens(private val store: RunRepository) : RunRepository by store {
    override fun observeRuns(): Flow<List<Run>> = flowOf(emptyList())
}
