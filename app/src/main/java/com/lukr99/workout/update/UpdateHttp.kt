package com.lukr99.workout.update

import java.io.InputStream

/**
 * The network side of the updater. The real one ([HttpsUpdateHttp]) refuses anything but HTTPS;
 * tests pass a fake. [open] returns a stream the caller closes.
 */
interface UpdateHttp {
    fun getText(url: String, maxBytes: Int): String

    fun open(url: String): Download

    /** An open response body. [contentLength] is -1 when the server does not say. */
    class Download(val contentLength: Long, val body: InputStream, private val onClose: () -> Unit) : AutoCloseable {
        override fun close() {
            runCatching { body.close() }
            onClose()
        }
    }
}
