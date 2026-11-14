package com.lukr99.workout.ui.about

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutContentTest {
    @Test
    fun versionLabelDropsALeadingV() {
        assertEquals("Version 2.11.0", AboutContent.versionLabel("2.11.0"))
        assertEquals("Version 2.11.0-dev", AboutContent.versionLabel("v2.11.0-dev"))
        assertEquals("Version", AboutContent.versionLabel("  "))
    }

    @Test
    fun bothEditionsCreditTheLicenceTypeIconsAndLibraries() {
        AboutEdition.entries.forEach { edition ->
            val kinds = AboutContent.credits(edition).map { it.kind }
            assertTrue(kinds.containsAll(listOf(AboutCreditKind.Source, AboutCreditKind.Type, AboutCreditKind.Icons, AboutCreditKind.Libraries)))
            assertEquals("PolyForm Noncommercial 1.0.0", AboutContent.credits(edition).first { it.kind == AboutCreditKind.Source }.licence)
        }
    }

    @Test
    fun theWebEditionLeavesOutWhatOnlyAndroidLoads() {
        val web = AboutContent.credits(AboutEdition.Web).map { it.kind }
        assertFalse(AboutCreditKind.Maps in web)
        assertFalse(AboutCreditKind.Pictures in web)
        val android = AboutContent.credits(AboutEdition.Android).map { it.kind }
        assertTrue(AboutCreditKind.Maps in android)
        assertTrue(AboutCreditKind.Pictures in android)
    }

    @Test
    fun privacyLinesDifferByEdition() {
        assertTrue(AboutContent.privacy(AboutEdition.Android).any { "phone" in it })
        assertTrue(AboutContent.privacy(AboutEdition.Web).any { "browser" in it })
        assertFalse(AboutContent.privacy(AboutEdition.Web).any { "phone" in it })
    }

    @Test
    fun noTextUsesAnEmDash() {
        val text = AboutEdition.entries.flatMap { edition ->
            AboutContent.privacy(edition) + AboutContent.summary(edition) + AboutContent.editionName(edition) +
                AboutContent.credits(edition).flatMap { listOf(it.label, it.credit, it.licence) }
        } + AboutContent.TAGLINE
        text.forEach { assertFalse(it, '\u2014' in it) }
    }
}
