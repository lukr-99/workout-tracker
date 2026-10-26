package com.lukr99.workout.data.transfer

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Photos are stored in backups as `java.util.Base64` text, so the Kotlin codec must read and write the same. */
@OptIn(ExperimentalEncodingApi::class)
class PhotoBase64Test {
    @Test
    fun encodesLikeJavaWithPaddingAndNoLineBreaks() {
        val random = Random(3)
        for (length in listOf(0, 1, 2, 3, 4, 5, 57, 58, 76, 77, 1_000, 200_000)) {
            val bytes = random.nextBytes(length)
            val expected = java.util.Base64.getEncoder().encodeToString(bytes)
            assertEquals("length $length", expected, Base64.Default.encode(bytes))
        }
    }

    @Test
    fun decodesWhatJavaWrote() {
        val random = Random(4)
        for (length in listOf(0, 1, 2, 3, 100, 100_000)) {
            val bytes = random.nextBytes(length)
            val text = java.util.Base64.getEncoder().encodeToString(bytes)
            assertArrayEquals("length $length", bytes, Base64.Default.decode(text))
        }
    }
}
