package com.lukr99.workout.data.routing

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

/**
 * OSRM implementation. **R3 dev:** the keyless public demo server (`router.project-osrm.org`,
 * `foot` profile) — matching the "keyless for dev" pattern of the map tiles; swap [baseUrl] for a
 * self-hosted OSRM/Valhalla for production. The HTTP lives here; the response decode is the pure,
 * unit-tested [OsrmRouteParser].
 */
class OsrmRoutingClient(
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val profile: String = "foot",
) : RoutingClient {

    override suspend fun snap(waypoints: List<LatLon>): SnappedRoute? {
        if (waypoints.size < 2) return null
        val coords = waypoints.joinToString(";") { "${it.lon},${it.lat}" }
        val url = "$baseUrl/route/v1/$profile/$coords?overview=full&geometries=polyline&steps=false"
        val body = get(url) ?: return null
        return OsrmRouteParser.parse(body)
    }

    private suspend fun get(url: String): String? = suspendCancellableCoroutine { cont ->
        val call = client.newCall(Request.Builder().url(url).get().build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val text = if (it.isSuccessful) it.body?.string() else null
                    if (cont.isActive) cont.resume(text)
                }
            }
        })
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://router.project-osrm.org"
    }
}
