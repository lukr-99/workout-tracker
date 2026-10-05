package com.lukr99.workout.update

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The signer check against real APKs on the device. */
class PackageSignatureCheckTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val check = PackageSignatureCheck(instrumentation.targetContext)

    @Test
    fun theInstalledAppsOwnApkMatches() {
        assertTrue(check.sameSigner(File(instrumentation.targetContext.applicationInfo.sourceDir)))
    }

    @Test
    fun anotherPackageDoesNotMatchEvenWithTheSameKey() {
        // The test APK is signed with the same debug key but is a different package.
        assertFalse(check.sameSigner(File(instrumentation.context.applicationInfo.sourceDir)))
    }

    @Test
    fun aFileThatIsNotAnApkDoesNotMatch() {
        val junk = File(instrumentation.targetContext.cacheDir, "not-an.apk").apply { writeText("hello") }
        try {
            assertFalse(check.sameSigner(junk))
        } finally {
            junk.delete()
        }
    }
}
