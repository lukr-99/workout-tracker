package com.lukr99.workout.data.routing

import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.RoutePoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

/** A lat/lon waypoint tapped on the planner map. */
data class LatLon(val lat: Double, val lon: Double)

/** A road/path-snapped route: ordered points + the provider's distance (metres). */
data class SnappedRoute(
    val points: List<RoutePoint>,
    val distanceMeters: Double,
)

/**
 * Snaps planner waypoints to roads/paths. The **only** class that talks to a routing provider, so the
 * provider (currently keyless OSRM) can be swapped without touching the planner UI. Live-run tracking
 * needs no routing — this is used only when planning/saving a [com.lukr99.workout.domain.run.Route].
 */
interface RoutingClient {
    /** Snap [waypoints] into a single walked/run route, or null if none could be found. */
    suspend fun snap(waypoints: List<LatLon>): SnappedRoute?
}

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

/** Pure decode of an OSRM `/route` response into a [SnappedRoute]. Testable without a network call. */
object OsrmRouteParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(body: String): SnappedRoute? {
        val response = runCatching { json.decodeFromString(OsrmResponse.serializer(), body) }.getOrNull()
            ?: return null
        if (response.code != "Ok") return null
        val route = response.routes.firstOrNull() ?: return null
        val decoded = Polyline.decode(route.geometry)
        if (decoded.size < 2) return null
        val points = decoded.mapIndexed { i, (lat, lon) -> RoutePoint(seq = i, lat = lat, lon = lon) }
        val distance = route.distance.takeIf { it > 0 } ?: Pace.pathDistanceMeters(decoded)
        return SnappedRoute(points = points, distanceMeters = distance)
    }

    @Serializable
    private data class OsrmResponse(
        val code: String = "",
        val routes: List<OsrmRoute> = emptyList(),
    )

    @Serializable
    private data class OsrmRoute(
        val geometry: String = "",
        val distance: Double = 0.0,
    )
}
