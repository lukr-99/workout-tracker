package com.lukr99.workout.ui.i18n

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Android's own `strings.xml` (the app name, notifications) has a Czech value for every key. */
class AndroidStringsTest {

    private fun keys(path: String): Map<String, String> {
        val text = File(path).readText()
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun everyEnglishStringHasACzechValue() {
        val english = keys("src/main/res/values/strings.xml")
        val czech = keys("src/main/res/values-cs/strings.xml")
        val missing = english.keys - czech.keys
        val extra = czech.keys - english.keys
        assertTrue("No Czech value for: $missing", missing.isEmpty())
        assertTrue("Czech strings with no English key: $extra", extra.isEmpty())
        assertTrue("Empty Czech values: ${czech.filterValues { it.isBlank() }.keys}", czech.values.none { it.isBlank() })
        assertEquals(english.size, czech.size)
    }
}
