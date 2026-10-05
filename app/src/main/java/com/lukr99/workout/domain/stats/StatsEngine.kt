package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.query.WorkoutDataPoint
import com.lukr99.workout.domain.query.WorkoutQueryEngine
import java.time.ZoneId

/**
 * Extensible multi-dimensional statistics engine. Metrics and dimensions are addressed by stable
 * string keys and supplied through providers, so a future feature/plugin can add a metric without
 * changing this engine or the request/response contract.
 */
class StatsEngine(
    metricProviders: Iterable<MetricProvider> = BuiltInMetrics.all,
    dimensionProviders: Iterable<DimensionProvider> = BuiltInDimensions.all,
) {
    private val metrics = metricProviders.associateBy(MetricProvider::key)
    private val dimensions = dimensionProviders.associateBy(DimensionProvider::key)

    fun calculate(
        sessions: Iterable<WorkoutSession>,
        request: StatsRequest = StatsRequest(),
    ): StatsReport {
        val zone = runCatching { ZoneId.of(request.timeZoneId) }.getOrDefault(ZoneId.systemDefault())
        val selection = WorkoutQueryEngine.select(sessions, request.query)
        val requestedMetrics = request.metrics.distinct()
        val requestedDimensions = request.dimensions.distinct()
        val unknownMetrics = requestedMetrics.filterNot(metrics::containsKey)
        val unknownDimensions = requestedDimensions.filterNot(dimensions::containsKey)
        require(unknownMetrics.isEmpty()) { "Unknown metric(s): ${unknownMetrics.joinToString()}" }
        require(unknownDimensions.isEmpty()) { "Unknown dimension(s): ${unknownDimensions.joinToString()}" }

        val groups = if (requestedDimensions.isEmpty()) {
            linkedMapOf(emptyMap<String, String>() to selection.points)
        } else {
            linkedMapOf<Map<String, String>, MutableList<WorkoutDataPoint>>().apply {
                selection.points.forEach { point ->
                    dimensionGroups(point, requestedDimensions, zone).forEach { group ->
                        getOrPut(group) { mutableListOf() } += point
                    }
                }
            }
        }

        val rows = groups.map { (group, points) ->
            StatsRow(
                dimensions = group,
                metrics = requestedMetrics.associateWith { key ->
                    metrics.getValue(key).calculate(points)
                },
                sampleSize = points.size,
            )
        }.let { unsorted ->
            val comparator = request.sort.mapNotNull { sort ->
                when {
                    sort.key in requestedMetrics -> compareBy<StatsRow> { it.metrics[sort.key]?.value ?: 0.0 }
                    sort.key in requestedDimensions -> compareBy { it.dimensions[sort.key].orEmpty() }
                    else -> null
                }?.let { if (sort.descending) it.reversed() else it }
            }.reduceOrNull(Comparator<StatsRow>::thenComparing)
            comparator?.let(unsorted::sortedWith) ?: unsorted
        }

        return StatsReport(
            request = request,
            rows = rows,
            matchedSessions = selection.matchedSessionIds.size,
            matchedEntries = selection.matchedEntryIds.size,
            matchedPoints = selection.points.size,
            availableMetricKeys = metrics.keys,
            availableDimensionKeys = dimensions.keys,
        )
    }

    private fun dimensionGroups(
        point: WorkoutDataPoint,
        requestedDimensions: List<String>,
        zone: ZoneId,
    ): List<Map<String, String>> = requestedDimensions.fold(listOf(emptyMap())) { groups, key ->
        val provider = dimensions.getValue(key)
        val values = if (provider is ExpandingDimensionProvider) {
            provider.resolveValues(point, zone).ifEmpty { setOf("") }
        } else {
            setOf(provider.resolve(point, zone))
        }
        groups.flatMap { group -> values.map { value -> group + (key to value) } }
    }
}

internal fun Iterable<Double>.averageOrZero(): Double {
    var count = 0
    var sum = 0.0
    forEach {
        count++
        sum += it
    }
    return if (count == 0) 0.0 else sum / count
}
