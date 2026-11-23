package com.lukr99.workout.ui.i18n

import com.lukr99.workout.settings.LanguageSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every English message has a real Czech one with the same values to fill in. */
class MessageCatalogTest {

    private val catalogs = AllCatalogs.all

    @Test
    fun everyCatalogIsListedOnlyOnce() {
        assertEquals(catalogs.size, catalogs.map { it.catalogName }.toSet().size)
    }

    @Test
    fun noEnglishMessageIsMissingItsCzechText() {
        val problems = mutableListOf<String>()
        for (catalog in catalogs) for (entry in catalog.entries) {
            entry.msg?.let { m ->
                if (m.en.isBlank()) problems += "${catalog.catalogName}: empty English text"
                if (m.cs.isBlank()) problems += "${catalog.catalogName}: no Czech for \"${m.en}\""
                else if (m.cs == m.en && !entry.sameInCzech) problems += "${catalog.catalogName}: Czech is still English for \"${m.en}\""
                if (placeholders(m.en).sorted() != placeholders(m.cs).sorted()) {
                    problems += "${catalog.catalogName}: values differ between English and Czech in \"${m.en}\""
                }
            }
            entry.plural?.let { p ->
                val czech = listOf(p.csOne, p.csFew, p.csMany)
                if (czech.any { it.isBlank() }) problems += "${catalog.catalogName}: a Czech form is empty for \"${p.enOther}\""
                // {n} may be left out of a form ("1 set" can be "one set"); every other value must match.
                fun values(t: String) = placeholders(t).filter { it != "n" }.sorted()
                val english = setOf(values(p.enOne), values(p.enOther))
                val cz = czech.map { values(it) }.toSet()
                if (english.size != 1 || cz.size != 1 || english != cz) {
                    problems += "${catalog.catalogName}: values differ between the forms of \"${p.enOther}\""
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun czechUsesOneFewAndManyForms() {
        val days = CommonText.daysAgo
        assertEquals("před 1 dnem", days.get(Language.Czech, 1))
        assertEquals("před 2 dny", days.get(Language.Czech, 2))
        assertEquals("před 4 dny", days.get(Language.Czech, 4))
        assertEquals("před 5 dny", days.get(Language.Czech, 5))
        assertEquals("před 0 dny", days.get(Language.Czech, 0))
        assertEquals("před 11 dny", days.get(Language.Czech, 11))
        assertEquals("před 22 dny", days.get(Language.Czech, 22))
    }

    @Test
    fun englishUsesOneAndOther() {
        assertEquals("1 day ago", CommonText.daysAgo.get(Language.English, 1))
        assertEquals("0 days ago", CommonText.daysAgo.get(Language.English, 0))
        assertEquals("3 days ago", CommonText.daysAgo.get(Language.English, 3))
    }

    @Test
    fun valuesAreFilledIn() {
        assertEquals("Version 2.12.0, who made it, feedback and credits.", SettingsText.aboutLinkHint.get(Language.English, "version" to "2.12.0"))
        assertTrue(SettingsText.aboutLinkHint.get(Language.Czech, "version" to "2.12.0").startsWith("Verze 2.12.0"))
    }

    @Test
    fun theDeviceLanguageSelectsCzechOrFallsBackToEnglish() {
        assertEquals(Language.Czech, Language.fromTag("cs-CZ"))
        assertEquals(Language.Czech, Language.fromTag("cs"))
        assertEquals(Language.English, Language.fromTag("de-DE"))
        assertEquals(Language.English, Language.fromTag(""))
    }

    @Test
    fun theSettingOverridesTheDeviceLanguage() {
        assertEquals(Language.Czech, Language.resolve(LanguageSetting.System, Language.Czech))
        assertEquals(Language.English, Language.resolve(LanguageSetting.English, Language.Czech))
        assertEquals(Language.Czech, Language.resolve(LanguageSetting.Czech, Language.English))
    }
}
