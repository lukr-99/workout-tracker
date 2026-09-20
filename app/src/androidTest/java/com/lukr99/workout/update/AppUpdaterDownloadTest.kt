package com.lukr99.workout.update

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * Covers the v2.5.1 update failure. A dropped connection ends the response stream without raising
 * anything, so the old `download()` returned a truncated APK, which the package installer then
 * rejected as corrupt — reported as "the Android screen opened but failed". The download must
 * reject a short transfer itself, and must never leave a partial file where [AppUpdater.install]
 * could hand it to the installer.
 *
 * Served from a loopback socket so the test never touches the network.
 */
class AppUpdaterDownloadTest {
    private lateinit var server: ServerSocket
    private lateinit var updater: AppUpdater

    private val updatesDir: File
        get() = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "updates",
        )

    @Before
    fun setUp() {
        server = ServerSocket(0)
        updater = AppUpdater(
            InstrumentationRegistry.getInstrumentation().targetContext,
            owner = "lukr-99",
            repo = "workout-tracker",
        )
    }

    @After
    fun tearDown() {
        server.close()
        updatesDir.deleteRecursively()
    }

    @Test
    fun rejectsATransferShorterThanTheAdvertisedAsset() {
        // GitHub says the asset is 4096 bytes; the transfer delivers 1024 and closes cleanly.
        respondWith(status = "200 OK", body = ByteArray(1024))

        val failure = runCatching { download(expectedSize = 4096) }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertTrue(
            "the message should name the shortfall, was: ${failure?.message}",
            failure?.message.orEmpty().contains("stopped early"),
        )
    }

    @Test
    fun leavesNoPartialFileForTheInstallerAfterAShortTransfer() {
        respondWith(status = "200 OK", body = ByteArray(1024))

        runCatching { download(expectedSize = 4096) }

        val leftovers = updatesDir.listFiles().orEmpty().map { it.name }
        assertTrue("a partial download survived: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun rejectsANonSuccessResponse() {
        respondWith(status = "404 Not Found", body = ByteArray(0))

        val failure = runCatching { download(expectedSize = 4096) }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertTrue(
            "the message should name the status, was: ${failure?.message}",
            failure?.message.orEmpty().contains("404"),
        )
    }

    @Test
    fun keepsACompleteTransferAndReportsProgress() {
        val body = ByteArray(4096) { (it % 251).toByte() }
        respondWith(status = "200 OK", body = body)
        val seen = mutableListOf<Float>()

        val apk = download(expectedSize = 4096, onProgress = { seen += it })

        assertEquals(4096L, apk.length())
        assertTrue("progress should have been reported", seen.isNotEmpty())
        assertEquals(1f, seen.last(), 0.001f)
        assertFalse("a .part file survived", File(apk.path + ".part").exists())
    }

    private fun download(
        expectedSize: Long,
        onProgress: (Float) -> Unit = {},
    ): File = runBlocking {
        updater.download(
            AppRelease(
                versionName = "9.9.9",
                tag = "v9.9.9",
                apkUrl = "http://127.0.0.1:${server.localPort}/update.apk",
                notes = "",
                sizeBytes = expectedSize,
            ),
            onProgress,
        )
    }

    /** Answers exactly one request with [status] and [body], then closes — like a dropped peer. */
    private fun respondWith(status: String, body: ByteArray) {
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket ->
                    socket.getInputStream().read(ByteArray(REQUEST_BYTES))
                    socket.getOutputStream().run {
                        write(
                            (
                                "HTTP/1.1 $status\r\n" +
                                    "Content-Length: ${body.size}\r\n" +
                                    "Connection: close\r\n\r\n"
                                ).toByteArray(),
                        )
                        write(body)
                        flush()
                    }
                }
            }
        }
    }

    private companion object {
        /** Enough for the request line and headers HttpURLConnection sends; the body is empty. */
        const val REQUEST_BYTES = 4096
    }
}
