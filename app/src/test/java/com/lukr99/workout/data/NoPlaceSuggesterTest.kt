package com.lukr99.workout.data

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull

class NoPlaceSuggesterTest {
    @Test
    fun neverSuggestsAndNeverHasPermission() = runTest {
        assertNull(NoPlaceSuggester.suggest(emptyList()))
        assertNull(NoPlaceSuggester.currentFix())
        assertFalse(NoPlaceSuggester.hasPermission())
    }
}
