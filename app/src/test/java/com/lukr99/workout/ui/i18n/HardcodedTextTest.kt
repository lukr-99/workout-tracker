package com.lukr99.workout.ui.i18n

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * New screen text must go through a catalog. This looks for English words written straight into a
 * `Text(...)`, `title =`, `label =`, `contentDescription =` and similar calls under `ui/`.
 *
 * `hardcoded-text-baseline.txt` lists the files that still have some, with their count. It is debt:
 * a count may only go down, and a file you finish comes off the list.
 */
class HardcodedTextTest {

    private val shown = Regex(
        """(Text\(\s*|\b(text|title|label|subtitle|description|placeholder|contentDescription|hint|message|summary|body|headline)\s*=\s*)"([^"\\]|\\.)*[A-Za-z]{3,}([^"\\]|\\.)*"""",
    )

    private fun count(file: File): Int = file.readLines().count { line ->
        val t = line.trim()
        !t.startsWith("//") && !t.startsWith("*") && !t.startsWith("import") && shown.containsMatchIn(t)
    }

    private fun scan(): Map<String, Int> {
        val root = File("src/main/java/com/lukr99/workout/ui")
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.path.replace('\\', '/').contains("/ui/i18n/") }
            .associate { it.relativeTo(root).path.replace('\\', '/') to count(it) }
            .filterValues { it > 0 }
    }

    private fun baseline(): Map<String, Int> =
        File("src/test/resources/hardcoded-text-baseline.txt").readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { it.substringBefore(' ') to it.substringAfter(' ').trim().toInt() }

    @Test
    fun noNewTextIsWrittenStraightIntoTheScreens() {
        val allowed = baseline()
        val problems = scan().filter { (file, n) -> n > (allowed[file] ?: 0) }
            .map { (file, n) -> "$file has $n hard-coded texts, the baseline allows ${allowed[file] ?: 0}" }
        assertTrue("Move the text into a catalog in ui/i18n:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theBaselineOnlyShrinks() {
        val now = scan()
        val stale = baseline().filter { (file, n) -> (now[file] ?: 0) < n }
            .map { (file, n) -> "$file now has ${now[file] ?: 0}, lower the baseline from $n" }
        assertTrue("Lower the numbers in hardcoded-text-baseline.txt:\n" + stale.joinToString("\n"), stale.isEmpty())
    }

    /** Set EMBER_PRINT_TEXT_BASELINE=1 and run this test to write a fresh baseline file. */
    @Test
    fun printBaselineWhenAsked() {
        if (System.getenv("EMBER_PRINT_TEXT_BASELINE") != "1") return
        File("src/test/resources/hardcoded-text-baseline.txt").writeText(
            "# Files under ui/ that still write English straight into the screen. May only shrink.\n" +
                scan().toSortedMap().entries.joinToString("\n", postfix = "\n") { "${it.key} ${it.value}" },
        )
    }
}
