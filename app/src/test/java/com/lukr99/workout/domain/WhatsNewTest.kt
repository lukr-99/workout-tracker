package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    private val notes = listOf(
        WhatsNewNote(10, "Ten", listOf("a")),
        WhatsNewNote(12, "Twelve", listOf("b")),
    )

    @Test
    fun aFreshInstallShowsNothing() {
        assertNull(WhatsNew.due(lastSeen = null, current = 10, freshInstall = true, all = notes))
    }

    @Test
    fun anUpdateFromBeforeTheCardExistedShowsTheCurrentNote() {
        assertEquals(10, WhatsNew.due(lastSeen = null, current = 10, freshInstall = false, all = notes)?.versionCode)
    }

    @Test
    fun aDismissedNoteStaysAway() {
        assertNull(WhatsNew.due(lastSeen = 10, current = 10, freshInstall = false, all = notes))
        assertNull(WhatsNew.due(lastSeen = 10, current = 11, freshInstall = false, all = notes))
    }

    @Test
    fun skippingReleasesShowsTheNewestNoteOnly() {
        assertEquals(12, WhatsNew.due(lastSeen = 9, current = 13, freshInstall = false, all = notes)?.versionCode)
    }

    @Test
    fun aNoteForALaterReleaseWaitsForIt() {
        assertNull(WhatsNew.due(lastSeen = null, current = 9, freshInstall = false, all = notes))
        assertEquals(10, WhatsNew.due(lastSeen = 9, current = 11, freshInstall = false, all = notes)?.versionCode)
    }

    @Test
    fun theShippedNotesAreShortAndPlain() {
        assertEquals(WhatsNew.notes.size, WhatsNew.notes.map { it.versionCode }.toSet().size)
        WhatsNew.notes.forEach { note ->
            assertTrue(note.title, note.lines.size in 1..5)
            (note.lines + note.title).forEach { line ->
                assertTrue(line, '—' !in line && line.length <= 90)
            }
        }
    }
}
