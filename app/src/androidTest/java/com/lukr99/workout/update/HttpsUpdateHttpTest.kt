package com.lukr99.workout.update

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.security.MessageDigest
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The real [HttpURLConnection][java.net.HttpURLConnection] path, served from a loopback socket so
 * the test never touches the network. A dropped connection ends the stream without an error; the
 * download must notice that itself (docs/pitfalls.md, 2.5.2). Plain HTTP is refused except on
 * loopback in this test.
 */
class HttpsUpdateHttpTest {
    private lateinit var server: ServerSocket
    private val updatesDir: File
        get() = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "updates-test")

    @Before
    fun setUp() {
        server = ServerSocket(0)
    }

    @After
    fun tearDown() {
        server.close()
        updatesDir.deleteRecursively()
    }

    @Test
    fun refusesPlainHttpOutsideTheTestLoopback() {
        val failure = runCatching { HttpsUpdateHttp("test").getText("http://example.com/x", 100) }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("HTTPS"))
    }

    @Test
    fun rejectsATransferThatEndsEarlyAndLeavesNoFile() {
        respondWith("200 OK", ByteArray(1024))

        val failure = runCatching { download(advertised = 4096, sha256 = "0".repeat(64)) }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("stopped early"))
        assertTrue(updatesDir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rejectsANonSuccessResponse() {
        respondWith("404 Not Found", ByteArray(0))

        val failure = runCatching { download(advertised = 4096, sha256 = "0".repeat(64)) }.exceptionOrNull()

        assertTrue("expected an IOException, got $failure", failure is IOException)
        assertTrue(failure?.message.orEmpty().contains("404"))
    }

    @Test
    fun keepsACompleteVerifiedTransfer() {
        val body = ByteArray(4096) { (it % 251).toByte() }
        respondWith("200 OK", body)
        val digest = MessageDigest.getInstance("SHA-256").digest(body).joinToString("") { "%02x".format(it) }

        val apk = download(advertised = 4096, sha256 = digest)

        assertEquals(4096L, apk.length())
        assertTrue(apk.readBytes().contentEquals(body))
    }

    private fun download(advertised: Long, sha256: String): File = runBlocking {
        val http = HttpsUpdateHttp("test", allowLoopbackHttp = true)
        VerifiedDownloader(http, updatesDir).download(
            ReleaseAsset("Ember-9.9.9.apk", "http://127.0.0.1:${server.localPort}/Ember-9.9.9.apk", advertised),
            sha256,
        )
    }

    /** Answers exactly one request with [status] and [body], then closes, like a dropped peer. */
    private fun respondWith(status: String, body: ByteArray) {
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket ->
                    socket.getInputStream().read(ByteArray(4096))
                    socket.getOutputStream().run {
                        write("HTTP/1.1 $status\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(body)
                        flush()
                    }
                }
            }
        }
    }
}
