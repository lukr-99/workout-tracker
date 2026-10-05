package com.lukr99.workout.data.routing

import com.lukr99.workout.domain.run.Pace
import com.lukr99.workout.domain.run.Polyline
import com.lukr99.workout.domain.run.RoutePoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
