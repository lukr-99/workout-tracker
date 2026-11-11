package com.lukr99.workout.data.claude

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The phone's side of the Claude server's `/v1` API (cloud/src/http/ApiRouter.ts): setup, share
 * requests, sharing, Claude's proposals and connector codes. Every call but [setUp] uses the link
 * in [store]. Calls run on IO, time out, and fail with [ClaudeApiException].
 */
class ClaudeApi(
    private val store: ClaudeLinkStore,
    private val http: OkHttpClient = defaultClient(),
) {
    /** Registers this phone with the server's setup secret and keeps the token it gets. */
    suspend fun setUp(serverUrl: String, setupSecret: String, deviceName: String): ClaudeLink {
        val base = normalizeServerUrl(serverUrl)
        val body = jsonBody(SetupRequest.serializer(), SetupRequest(setupSecret, deviceName))
        val answer = execute(Request.Builder().url("$base/v1/devices").post(body).build(), SetupResponse.serializer())
        return ClaudeLink(base, answer.token).also { store.setLink(it) }
    }

    suspend fun setPushToken(pushToken: String?) {
        send("PUT", "/v1/push-token", jsonBody(PushTokenRequest.serializer(), PushTokenRequest(pushToken)))
    }

    suspend fun shareRequests(): List<ShareRequest> = get("/v1/share-requests", ShareRequestsResponse.serializer()).requests

    /** Answers [requestId] with [share], or shares unasked when it is null. */
    suspend fun share(requestId: String?, share: ClaudeShare): ShareStatus {
        val path = if (requestId == null) "/v1/share" else "/v1/share-requests/$requestId/share"
        val body = ClaudeShareBuilder.toJson(share).toRequestBody(JSON_TYPE)
        return send("POST", path, body, ShareResponse.serializer()).let { ShareStatus(it.sharedAt, it.keepUntil) }
    }

    suspend fun decline(requestId: String) {
        send("POST", "/v1/share-requests/$requestId/decline", EMPTY_BODY)
    }

    suspend fun shareStatus(): ShareStatus? = get("/v1/share", ShareStatusResponse.serializer()).share

    suspend fun forgetShare() {
        send("DELETE", "/v1/share", null)
    }

    suspend fun pendingProposals(): List<Proposal> = get("/v1/proposals?status=pending", ProposalsResponse.serializer()).proposals

    suspend fun decide(proposalId: String, accepted: Boolean) {
        send("POST", "/v1/proposals/$proposalId/decision", jsonBody(DecisionRequest.serializer(), DecisionRequest(if (accepted) "accepted" else "rejected")))
    }

    suspend fun connectorCode(): ConnectorCode = send("POST", "/v1/connector-codes", EMPTY_BODY, ConnectorCode.serializer())

    suspend fun devices(): List<ClaudeDevice> = get("/v1/devices", DevicesResponse.serializer()).devices

    suspend fun revokeDevice(id: String) {
        send("DELETE", "/v1/devices/$id", null)
    }

    private suspend fun <T> get(path: String, serializer: KSerializer<T>): T = send("GET", path, null, serializer)

    private suspend fun send(method: String, path: String, body: RequestBody?) {
        execute<Unit>(request(method, path, body), null)
    }

    private suspend fun <T> send(method: String, path: String, body: RequestBody?, serializer: KSerializer<T>): T =
        execute(request(method, path, body), serializer)

    private suspend fun request(method: String, path: String, body: RequestBody?): Request {
        val link = store.link.first() ?: throw ClaudeApiException(0, "not_connected", "Connect Ember to your Claude server first.")
        return Request.Builder()
            .url(link.serverUrl + path)
            .header("Authorization", "Bearer ${link.token}")
            .method(method, body)
            .build()
    }

    private suspend fun <T> execute(request: Request, serializer: KSerializer<T>?): T = withContext(Dispatchers.IO) {
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw ClaudeApiException(0, "network", "Could not reach the Claude server: ${e.message ?: "no connection"}.")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val error = runCatching { JSON.decodeFromString(ErrorResponse.serializer(), text).error }.getOrNull()
                throw ClaudeApiException(it.code, error?.code ?: "http_${it.code}", error?.message ?: "The Claude server answered ${it.code}.")
            }
            @Suppress("UNCHECKED_CAST")
            if (serializer == null) Unit as T else JSON.decodeFromString(serializer, text)
        }
    }

    private fun <T> jsonBody(serializer: KSerializer<T>, value: T): RequestBody =
        JSON.encodeToString(serializer, value).toRequestBody(JSON_TYPE)

    @Serializable
    private data class SetupRequest(val setupSecret: String, val name: String)

    @Serializable
    private data class SetupResponse(val deviceId: String, val token: String)

    @Serializable
    private data class PushTokenRequest(val pushToken: String?)

    @Serializable
    private data class ShareRequestsResponse(val requests: List<ShareRequest>)

    @Serializable
    private data class ShareResponse(val sharedAt: String, val keepUntil: String)

    @Serializable
    private data class ShareStatusResponse(val share: ShareStatus? = null)

    @Serializable
    private data class ProposalsResponse(val proposals: List<Proposal>)

    @Serializable
    private data class DecisionRequest(val decision: String)

    @Serializable
    private data class DevicesResponse(val devices: List<ClaudeDevice>)

    @Serializable
    private data class ErrorResponse(val error: ErrorBody)

    @Serializable
    private data class ErrorBody(val code: String, val message: String)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private val EMPTY_BODY = "{}".toRequestBody(JSON_TYPE)

        /** A share can be several megabytes on a slow phone connection. */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        /** An https address without a trailing slash; a bare host gets https. Plain http only for a local server. */
        fun normalizeServerUrl(serverUrl: String): String {
            var text = serverUrl.trim().trimEnd('/')
            if (!text.contains("://")) text = "https://$text"
            val local = Regex("^http://(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2)(:\\d+)?$")
            require(text.startsWith("https://") || local.matches(text)) {
                "Enter the server's https address, such as https://workout-tracker.you.workers.dev."
            }
            require(Regex("^https?://[^/\\s]+$").matches(text)) { "Enter just the server's address, without a path." }
            return text
        }
    }
}
