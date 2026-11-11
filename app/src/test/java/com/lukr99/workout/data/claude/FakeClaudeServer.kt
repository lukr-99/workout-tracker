package com.lukr99.workout.data.claude

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/** Plays the Claude server in tests: canned answers by "METHOD /path", and every request it was sent. */
class FakeClaudeServer {
    data class Sent(val method: String, val path: String, val authorization: String?, val body: String)

    private val answers = mutableMapOf<String, Pair<Int, String>>()
    val sent = mutableListOf<Sent>()

    fun answer(method: String, path: String, status: Int, body: String = "{}") {
        answers["$method $path"] = status to body
    }

    val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain -> respond(chain) }).build()

    private fun respond(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath + (request.url.encodedQuery?.let { "?$it" } ?: "")
        val body = request.body?.let { Buffer().also(it::writeTo).readUtf8() }.orEmpty()
        sent += Sent(request.method, path, request.header("Authorization"), body)
        val (status, answer) = answers["${request.method} $path"] ?: (404 to """{"error":{"code":"not_found","message":"Not found."}}""")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("fake")
            .body(answer.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
