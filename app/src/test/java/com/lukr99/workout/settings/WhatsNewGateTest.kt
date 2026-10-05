package com.lukr99.workout.settings

import com.lukr99.workout.domain.WhatsNewNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WhatsNewGateTest {
    private class FakeStore(start: Int?) : SeenVersionStore {
        val state = MutableStateFlow(start)
        override val seen: Flow<Int?> = state
        override suspend fun markSeen(versionCode: Int) {
            state.value = versionCode
        }
    }

    private val notes = listOf(WhatsNewNote(10, "Ten", listOf("a")))

    @Test
    fun gotItPutsTheCardAwayForThisVersion() = runBlocking {
        val store = FakeStore(start = 9)
        val gate = WhatsNewGate(store, current = 10, freshInstall = false, notes = notes)
        assertEquals("Ten", gate.due.first()?.title)

        gate.dismiss()

        assertEquals(10, store.state.value)
        assertNull(gate.due.first())
    }
}
