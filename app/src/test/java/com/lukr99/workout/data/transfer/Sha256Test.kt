package com.lukr99.workout.data.transfer

import java.security.MessageDigest
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Sha256Test {
    private fun reference(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun referenceHex(text: String): String =
        reference(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    @Test
    fun knownVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Sha256.hexOf(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Sha256.hexOf("abc"))
    }

    @Test
    fun matchesMessageDigestForEveryLengthAroundTheBlockEdges() {
        val random = Random(42)
        for (length in 0..300) {
            val bytes = random.nextBytes(length)
            assertArrayEquals("length $length", reference(bytes), Sha256.digest(bytes))
        }
    }

    @Test
    fun matchesMessageDigestForLongInputs() {
        val random = Random(7)
        for (length in listOf(1_000, 4_096, 65_537, 1_000_000)) {
            val bytes = random.nextBytes(length)
            assertArrayEquals("length $length", reference(bytes), Sha256.digest(bytes))
        }
    }

    @Test
    fun hexMatchesForTextWithNonAsciiCharacters() {
        val samples = listOf(
            "Push day|28400000",
            "Dřep s činkou ěščřžýáíé",
            "emoji 🏋️ and 中文",
            "a".repeat(10_000),
            "line\r\nbreak\ttab",
        )
        samples.forEach { assertEquals(it.take(20), referenceHex(it), Sha256.hexOf(it)) }
    }
}
