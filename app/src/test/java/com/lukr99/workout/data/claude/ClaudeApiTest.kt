package com.lukr99.workout.data.claude

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeApiTest {
    private val server = FakeClaudeServer()

    @Test
    fun setUpNormalizesTheAddressAndKeepsTheToken() = runTest {
        val store = MemoryClaudeLinkStore(link = null)
        server.answer("POST", "/v1/devices", 201, """{"deviceId":"d1","token":"wt1.new"}""")

        val link = ClaudeApi(store, server.client).setUp(" workout-tracker.you.workers.dev/ ", "the-secret", "Pixel")

        assertEquals(ClaudeLink("https://workout-tracker.you.workers.dev", "wt1.new"), link)
        assertEquals(link, store.link.first())
        assertEquals("""{"setupSecret":"the-secret","name":"Pixel"}""", server.sent.single().body)
        assertNull(server.sent.single().authorization)
    }

    @Test
    fun addressesMustBeHttpsOrALocalServer() {
        assertEquals("http://10.0.2.2:8799", ClaudeApi.normalizeServerUrl("http://10.0.2.2:8799"))
        assertThrows(IllegalArgumentException::class.java) { ClaudeApi.normalizeServerUrl("http://example.com") }
        assertThrows(IllegalArgumentException::class.java) { ClaudeApi.normalizeServerUrl("https://example.com/mcp") }
    }

    @Test
    fun callsCarryThePhoneToken() = runTest {
        server.answer("GET", "/v1/proposals?status=pending", 200, """{"proposals":[]}""")

        ClaudeApi(MemoryClaudeLinkStore(), server.client).pendingProposals()

        assertEquals("Bearer wt1.token", server.sent.single().authorization)
    }

    @Test
    fun aRefusalCarriesTheServersCodeAndMessage() = runTest {
        server.answer("POST", "/v1/connector-codes", 429, """{"error":{"code":"rate_limited","message":"Too many requests. Try again in a minute."}}""")

        val refusal = runCatching { ClaudeApi(MemoryClaudeLinkStore(), server.client).connectorCode() }.exceptionOrNull() as ClaudeApiException

        assertEquals(429, refusal.status)
        assertEquals("rate_limited", refusal.code)
        assertEquals("Too many requests. Try again in a minute.", refusal.message)
    }

    @Test
    fun nothingIsSentBeforeThePhoneIsConnected() = runTest {
        val refusal = runCatching { ClaudeApi(MemoryClaudeLinkStore(link = null), server.client).shareStatus() }.exceptionOrNull()

        assertTrue(refusal is ClaudeApiException && refusal.code == "not_connected")
        assertTrue(server.sent.isEmpty())
    }
}
