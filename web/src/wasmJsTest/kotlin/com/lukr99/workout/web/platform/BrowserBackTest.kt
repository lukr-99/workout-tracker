package com.lukr99.workout.web.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserBackTest {
    private var pushes = 0
    private var pop: () -> Unit = {}
    private val back = BrowserBack(pushEntry = { pushes++ }, listen = { pop = it })

    @Test
    fun pushesNothingWhileNoHandlerIsEnabled() {
        back.register(enabled = false) {}
        assertEquals(0, pushes)
    }

    @Test
    fun pushesOneEntryWhileAHandlerIsEnabled() {
        back.register(enabled = true) {}
        back.register(enabled = true) {}
        assertEquals(1, pushes)
    }

    @Test
    fun pushesWhenAHandlerGetsEnabled() {
        val handler = back.register(enabled = false) {}
        handler.enabled = true
        assertEquals(1, pushes)
    }

    @Test
    fun runsTheInnermostEnabledHandlerAndPutsTheEntryBack() {
        val calls = mutableListOf<String>()
        back.register(enabled = true) { calls += "screen" }
        back.register(enabled = true) { calls += "sheet" }
        pop()
        assertEquals(listOf("sheet"), calls)
        assertEquals(2, pushes)
    }

    @Test
    fun passesOverDisabledAndRemovedHandlers() {
        val calls = mutableListOf<String>()
        back.register(enabled = true) { calls += "screen" }
        val sheet = back.register(enabled = true) { calls += "sheet" }
        val menu = back.register(enabled = false) { calls += "menu" }
        pop()
        sheet.remove()
        pop()
        menu.remove()
        assertEquals(listOf("sheet", "screen"), calls)
    }

    @Test
    fun enablingAnInnerHandlerDoesNotMoveItPastALaterOne() {
        val calls = mutableListOf<String>()
        val first = back.register(enabled = false) { calls += "first" }
        back.register(enabled = true) { calls += "second" }
        first.enabled = true
        pop()
        assertEquals(listOf("second"), calls)
    }

    @Test
    fun doesNotPushAgainOnceNothingIsEnabled() {
        val handler = back.register(enabled = true) {}
        pop()
        handler.enabled = false
        pop()
        assertEquals(2, pushes)
    }
}
