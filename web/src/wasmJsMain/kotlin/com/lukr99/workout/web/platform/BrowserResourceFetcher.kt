package com.lukr99.workout.web.platform

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response

/**
 * Fetches files served beside the page, by path relative to it.
 *
 * A failed request (offline with nothing cached, or a 404) reads as null rather than throwing: the
 * callers know what to do when a file is not there.
 */
internal object BrowserResourceFetcher {
    suspend fun text(path: String): String? =
        response(path)?.text()?.await<JsString>()?.toString()

    suspend fun bytes(path: String): ByteArray? =
        response(path)?.arrayBuffer()?.await<ArrayBuffer>()?.toByteArray()

    private suspend fun response(path: String): Response? =
        runCatching { window.fetch(path).await<Response>() }.getOrNull()?.takeIf { it.ok }
}

/** Copies a JavaScript buffer into Kotlin memory; WebAssembly cannot read the JavaScript heap directly. */
private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { view[it] }
}
