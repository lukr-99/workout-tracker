package com.lukr99.workout.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorLinksTest {
    @Test
    fun profileLinksAreBuiltFromTheHandles() {
        assertEquals("https://www.instagram.com/lukas9krejci/", AuthorLinks.INSTAGRAM_URL)
        assertEquals("https://github.com/lukr-99", AuthorLinks.GITHUB_URL)
        assertEquals("Lukáš Krejčí", AuthorLinks.NAME)
    }

    @Test
    fun issuesPointAtThisRepository() {
        assertEquals("https://github.com/lukr-99/workout-tracker", AuthorLinks.REPOSITORY_URL)
        assertEquals("https://github.com/lukr-99/workout-tracker/issues", AuthorLinks.ISSUES_URL)
    }

    @Test
    fun everyLinkIsHttps() {
        listOf(AuthorLinks.INSTAGRAM_URL, AuthorLinks.GITHUB_URL, AuthorLinks.REPOSITORY_URL, AuthorLinks.ISSUES_URL)
            .forEach { assertTrue(it, it.startsWith("https://")) }
    }

    @Test
    fun initialsTakeTheFirstLetterOfTwoNames() {
        assertEquals("LK", AuthorLinks.initials())
        assertEquals("ŁŽ", AuthorLinks.initials("łukasz  žák extra"))
        assertEquals("E", AuthorLinks.initials("Ember"))
        assertEquals("", AuthorLinks.initials(""))
    }
}
