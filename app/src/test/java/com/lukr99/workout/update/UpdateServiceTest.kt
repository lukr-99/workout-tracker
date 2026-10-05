package com.lukr99.workout.update

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The whole updater chain with fakes for the network, the signer check and the installer. */
class UpdateServiceTest {
    private val dir: File = Files.createTempDirectory("updates").toFile()
    private val apkBytes = ByteArray(4096) { (it % 251).toByte() }
    private val http = FakeHttp()
    private var signerMatches = true
    private val launched = mutableListOf<File>()

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun service(current: String = "2.5.2", release: PublishedRelease = release()) = UpdateService(
        currentVersion = current,
        source = { release },
        http = http,
        downloader = VerifiedDownloader(http, dir),
        signatureCheck = { signerMatches },
        launcher = { launched += it },
    )

    private fun release(
        tag: String = "v2.6.0",
        assets: List<ReleaseAsset> = listOf(
            ReleaseAsset("Ember-2.6.0-debug.apk", "https://example.test/other.apk", 10),
            ReleaseAsset("Ember-2.6.0.apk", "https://example.test/apk", apkBytes.size.toLong()),
            ReleaseAsset("Ember-2.6.0.apk.sha256", "https://example.test/sum", 90),
        ),
    ) = PublishedRelease(tag = tag, body = "Notes", assets = assets)

    @Test
    fun aDevelopmentBuildNeverChecks() = runTest {
        assertEquals(UpdateCheck.DevelopmentBuild, service(current = "2.6.0-dev").check())
        assertTrue(http.requested.isEmpty())
    }

    @Test
    fun theSameOrAnOlderReleaseIsUpToDate() = runTest {
        assertEquals(UpdateCheck.UpToDate, service(current = "2.6.0").check())
        assertEquals(UpdateCheck.UpToDate, service(current = "2.7.0").check())
    }

    @Test
    fun aNewerReleaseOffersTheExactlyNamedApkNotTheFirstOne() = runTest {
        val check = service().check()

        val offer = (check as UpdateCheck.Available).offer
        assertEquals("2.6.0", offer.version)
        assertEquals("Ember-2.6.0.apk", offer.apk.name)
        assertEquals("Ember-2.6.0.apk.sha256", offer.checksum.name)
    }

    @Test
    fun aReleaseWithoutAChecksumIsNotOffered() = runTest {
        val noChecksum = release(assets = listOf(ReleaseAsset("Ember-2.6.0.apk", "https://example.test/apk", 1)))

        val check = service(release = noChecksum).check()

        assertTrue(check is UpdateCheck.NotVerifiable)
        assertTrue((check as UpdateCheck.NotVerifiable).reason.contains("checksum"))
    }

    @Test
    fun aVerifiedDownloadIsKeptAndHandedToTheInstaller() = runTest {
        http.serve(apkBytes, checksum = "${sha256(apkBytes)}  Ember-2.6.0.apk")
        val svc = service()
        val offer = (svc.check() as UpdateCheck.Available).offer
        val seen = mutableListOf<Float>()

        val apk = svc.download(offer) { seen += it }
        svc.install(apk)

        assertTrue(apk.readBytes().contentEquals(apkBytes))
        assertEquals(1f, seen.last(), 0.001f)
        assertEquals(listOf(apk), launched)
        assertFalse(File(apk.path + ".part").exists())
    }

    @Test
    fun aChecksumMismatchLeavesNoFile() = runTest {
        http.serve(apkBytes, checksum = "0".repeat(64))
        val svc = service()

        expectIo("checksum") { svc.download((svc.check() as UpdateCheck.Available).offer) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun aShortTransferIsRejected() = runTest {
        http.serve(apkBytes.copyOf(1024), checksum = sha256(apkBytes))
        val svc = service()

        expectIo("stopped early") { svc.download((svc.check() as UpdateCheck.Available).offer) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun aDifferentSignerIsRefusedAndDeleted() = runTest {
        http.serve(apkBytes, checksum = sha256(apkBytes))
        signerMatches = false
        val svc = service()

        expectIo("signed") { svc.download((svc.check() as UpdateCheck.Available).offer) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
        assertTrue(launched.isEmpty())
    }

    private suspend fun expectIo(fragment: String, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected an IOException mentioning '$fragment'")
        } catch (expected: IOException) {
            assertTrue("message was: ${expected.message}", expected.message.orEmpty().contains(fragment))
        }
    }

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class FakeHttp : UpdateHttp {
        val requested = mutableListOf<String>()
        private var apk = ByteArray(0)
        private var checksum = ""

        fun serve(apk: ByteArray, checksum: String) {
            this.apk = apk
            this.checksum = checksum
        }

        override fun getText(url: String, maxBytes: Int): String {
            requested += url
            return checksum
        }

        override fun open(url: String): UpdateHttp.Download {
            requested += url
            return UpdateHttp.Download(apk.size.toLong(), ByteArrayInputStream(apk)) {}
        }
    }
}
