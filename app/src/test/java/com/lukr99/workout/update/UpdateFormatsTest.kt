package com.lukr99.workout.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The small pure pieces: versions, the checksum file, and the GitHub release JSON. */
class UpdateFormatsTest {

    @Test
    fun devSuffixMarksADevelopmentBuild() {
        assertTrue(VersionPolicy.isDevelopmentVersion("2.5.2-dev"))
        assertTrue(VersionPolicy.isDevelopmentVersion(" 2.6.0-DEV "))
        assertFalse(VersionPolicy.isDevelopmentVersion("2.5.2"))
    }

    @Test
    fun isNewerComparesDottedNumbers() {
        assertTrue(VersionPolicy.isNewer("2.6.0", "2.5.2"))
        assertTrue(VersionPolicy.isNewer("v2.10.0", "2.9.9"))
        assertFalse(VersionPolicy.isNewer("2.5.2", "2.5.2"))
        assertFalse(VersionPolicy.isNewer("2.5.1", "2.5.2"))
    }

    @Test
    fun checksumFileAcceptsABareDigestOrASha256sumLine() {
        val digest = "AB".repeat(32)
        assertEquals(digest.lowercase(), Sha256File.parse(digest, "Ember-2.6.0.apk"))
        assertEquals(digest.lowercase(), Sha256File.parse("$digest  Ember-2.6.0.apk\n", "Ember-2.6.0.apk"))
        assertEquals(digest.lowercase(), Sha256File.parse("\n$digest *Ember-2.6.0.apk", "Ember-2.6.0.apk"))
    }

    @Test
    fun checksumFileRefusesAnotherFileOrGarbage() {
        val digest = "ab".repeat(32)
        assertTrue(runCatching { Sha256File.parse("$digest  Ember-2.5.0.apk", "Ember-2.6.0.apk") }.isFailure)
        assertTrue(runCatching { Sha256File.parse("not a digest", "Ember-2.6.0.apk") }.isFailure)
        assertTrue(runCatching { Sha256File.parse("", "Ember-2.6.0.apk") }.isFailure)
    }

    @Test
    fun gitHubReleaseJsonParsesWithUnknownFieldsIgnored() = runTest {
        val json = """
            {"tag_name":"v2.6.0","name":"Ember 2.6.0","body":"Notes","draft":false,"prerelease":false,
             "assets":[{"name":"Ember-2.6.0.apk","size":15907510,"content_type":"application/vnd.android.package-archive",
                        "browser_download_url":"https://github.com/lukr-99/workout-tracker/releases/download/v2.6.0/Ember-2.6.0.apk"}]}
        """.trimIndent()
        val http = object : UpdateHttp {
            var asked = ""
            override fun getText(url: String, maxBytes: Int): String = json.also { asked = url }
            override fun open(url: String): UpdateHttp.Download = error("not used")
        }

        val release = GitHubReleaseSource("lukr-99", "workout-tracker", http).latest()

        assertEquals("https://api.github.com/repos/lukr-99/workout-tracker/releases/latest", http.asked)
        assertEquals("2.6.0", release.version)
        assertEquals(15907510L, release.assets.single().size)
        assertTrue(release.assets.single().url.startsWith("https://github.com/"))
    }
}
