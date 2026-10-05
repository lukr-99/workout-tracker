package com.lukr99.workout.data.sync

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Cancellable OkHttp transport kept separate so paging/mapping can be tested without a network. */
class WgerApiClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : WgerPageSource {
    override suspend fun fetchPage(url: String): WgerPage = suspendCancellableCoroutine { result ->
        val call = client.newCall(Request.Builder().url(url).get().build())
        result.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (result.isActive) result.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) {
                            throw IOException("wger request failed with HTTP ${it.code}.")
                        }
                        val body = it.body?.string()
                            ?: throw IOException("wger returned an empty response body.")
                        if (result.isActive) {
                            result.resume(json.decodeFromString(WgerPage.serializer(), body))
                        }
                    }
                } catch (failure: Throwable) {
                    if (result.isActive) result.resumeWithException(failure)
                }
            }
        })
    }
}
