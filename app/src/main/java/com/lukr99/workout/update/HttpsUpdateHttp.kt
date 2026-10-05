package com.lukr99.workout.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * [UpdateHttp] over [HttpURLConnection] that only speaks HTTPS, before and after redirects.
 * [allowLoopbackHttp] exists for the instrumented download test, which serves from 127.0.0.1.
 */
class HttpsUpdateHttp(
    private val userAgent: String,
    private val allowLoopbackHttp: Boolean = false,
) : UpdateHttp {

    override fun getText(url: String, maxBytes: Int): String {
        val conn = connect(url, "application/vnd.github+json", readTimeoutMs = 15_000)
        try {
            val bytes = conn.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) throw IOException("The response from $url is too large.")
                }
                out.toByteArray()
            }
            return bytes.toString(Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    override fun open(url: String): UpdateHttp.Download {
        val conn = connect(url, "application/octet-stream", readTimeoutMs = 60_000)
        return UpdateHttp.Download(conn.contentLengthLong, conn.inputStream) { conn.disconnect() }
    }

    private fun connect(url: String, accept: String, readTimeoutMs: Int): HttpURLConnection {
        requireAllowed(url)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = readTimeoutMs
            // HttpURLConnection never follows a redirect that changes the scheme, so HTTPS stays HTTPS.
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("The server returned HTTP $code for $url.")
            requireAllowed(conn.url.toString())
            return conn
        } catch (failure: Throwable) {
            conn.disconnect()
            throw failure
        }
    }

    private fun requireAllowed(url: String) {
        val parsed = URL(url)
        val loopback = parsed.host == "127.0.0.1" || parsed.host == "localhost"
        if (parsed.protocol == "https" || (allowLoopbackHttp && loopback && parsed.protocol == "http")) return
        throw IOException("Updates are only downloaded over HTTPS, not from $url.")
    }
}
