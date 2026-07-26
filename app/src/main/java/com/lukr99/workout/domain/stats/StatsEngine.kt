package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.query.WorkoutDataPoint
import com.lukr99.workout.domain.query.WorkoutQuery
import com.lukr99.workout.domain.query.WorkoutQueryEngine
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

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

data class StatsRequest(
    val query: WorkoutQuery = WorkoutQuery(),
    val metrics: List<String> = listOf(
        MetricKeys.Workouts,
        MetricKeys.Sets,
        MetricKeys.Reps,
        MetricKeys.VolumeKg,
    ),
    val dimensions: List<String> = emptyList(),
    val timeZoneId: String = ZoneId.systemDefault().id,
    val sort: List<StatsSort> = emptyList(),
)

data class StatsSort(val key: String, val descending: Boolean = false)

data class StatsReport(
    val request: StatsRequest,
    val rows: List<StatsRow>,
    val matchedSessions: Int,
    val matchedEntries: Int,
    val matchedPoints: Int,
    val availableMetricKeys: Set<String>,
    val availableDimensionKeys: Set<String>,
)

data class StatsRow(
    val dimensions: Map<String, String>,
    val metrics: Map<String, MetricValue>,
    val sampleSize: Int,
)

data class MetricValue(
    val value: Double,
    val unit: MetricUnit = MetricUnit.Count,
)

enum class MetricUnit { Count, Kilograms, Seconds, Kilometers, Kilocalories, Ratio }

interface MetricProvider {
    val key: String
    fun calculate(points: List<WorkoutDataPoint>): MetricValue
}

interface DimensionProvider {
    val key: String
    fun resolve(point: WorkoutDataPoint, zoneId: ZoneId): String
}

/**
 * A dimension that can map one observation to several groups (for example one exercise loading
 * both a primary and secondary muscle). Existing single-value providers remain unchanged.
 */
interface ExpandingDimensionProvider : DimensionProvider {
    fun resolveValues(point: WorkoutDataPoint, zoneId: ZoneId): Set<String>
    override fun resolve(point: WorkoutDataPoint, zoneId: ZoneId): String =
        resolveValues(point, zoneId).firstOrNull().orEmpty()
}

object MetricKeys {
    const val Workouts = "workouts"
    const val Entries = "entries"
    const val Sets = "sets"
    const val Reps = "reps"
    const val VolumeKg = "volume_kg"
    const val AverageWeightKg = "average_weight_kg"
    const val MaxWeightKg = "max_weight_kg"
    const val BestE1rmKg = "best_e1rm_kg"
    const val SmoothedE1rmKg = "smoothed_e1rm_kg"
    const val DurationSeconds = "duration_seconds"
    const val TimedWorkSeconds = "timed_work_seconds"
    const val CardioDistanceKm = "cardio_distance_km"
    const val Calories = "calories"
    const val PrSets = "pr_sets"
    const val AverageRpe = "average_rpe"
}

object DimensionKeys {
    const val Exercise = "exercise"
    const val ExerciseId = "exercise_id"
    const val BodyPart = "body_part"
    const val Category = "category"
    const val Session = "session"
    const val SessionId = "session_id"
    const val Status = "status"
    const val SetType = "set_type"
    const val Day = "day"
    const val Week = "week"
    const val Month = "month"
    const val Year = "year"
}

object BuiltInMetrics {
    val all: List<MetricProvider> = listOf(
        metric(MetricKeys.Workouts, MetricUnit.Count) { points ->
            points.distinctBy { it.session.id }.size.toDouble()
        },
        metric(MetricKeys.Entries, MetricUnit.Count) { points ->
            points.mapNotNull { it.entry?.id }.distinct().size.toDouble()
        },
        metric(MetricKeys.Sets, MetricUnit.Count) { points ->
            points.count { it.strengthSet != null }.toDouble()
        },
        metric(MetricKeys.Reps, MetricUnit.Count) { points ->
            points.sumOf { it.strengthSet?.reps ?: 0 }.toDouble()
        },
        metric(MetricKeys.VolumeKg, MetricUnit.Kilograms) { points ->
            points.sumOf { point ->
                point.strengthSet?.let { it.weightKg * it.reps } ?: 0.0
            }
        },
        metric(MetricKeys.AverageWeightKg, MetricUnit.Kilograms) { points ->
            points.mapNotNull { it.strengthSet?.weightKg }.averageOrZero()
        },
        metric(MetricKeys.MaxWeightKg, MetricUnit.Kilograms) { points ->
            points.maxOfOrNull { it.strengthSet?.weightKg ?: 0.0 } ?: 0.0
        },
        metric(MetricKeys.BestE1rmKg, MetricUnit.Kilograms) { points ->
            points.maxOfOrNull {
                it.strengthSet?.let { set -> Estimates.epley(set.weightKg, set.reps) } ?: 0.0
            } ?: 0.0
        },
        SmoothedE1rmMetric,
        metric(MetricKeys.DurationSeconds, MetricUnit.Seconds) { points ->
            points.distinctBy { it.session.id }.sumOf { it.session.durationSeconds }.toDouble()
        },
        metric(MetricKeys.TimedWorkSeconds, MetricUnit.Seconds) { points ->
            points.sumOf {
                (it.strengthSet?.durationSeconds ?: 0) + (it.cardio?.durationSeconds ?: 0)
            }.toDouble()
        },
        metric(MetricKeys.CardioDistanceKm, MetricUnit.Kilometers) { points ->
            points.sumOf { it.cardio?.distanceKm ?: 0.0 }
        },
        metric(MetricKeys.Calories, MetricUnit.Kilocalories) { points ->
            points.sumOf { it.cardio?.calories ?: 0.0 }
        },
        metric(MetricKeys.PrSets, MetricUnit.Count) { points ->
            points.count { it.strengthSet?.isPr == true }.toDouble()
        },
        metric(MetricKeys.AverageRpe, MetricUnit.Ratio) { points ->
            points.mapNotNull { it.strengthSet?.rpe }.averageOrZero()
        },
    )

    private fun metric(
        key: String,
        unit: MetricUnit,
        block: (List<WorkoutDataPoint>) -> Double,
    ): MetricProvider = object : MetricProvider {
        override val key = key
        override fun calculate(points: List<WorkoutDataPoint>) = MetricValue(block(points), unit)
    }
}

/**
 * Chronological exponentially weighted e1RM trend. Each session contributes its best working-set
 * estimate so a high-set-count workout cannot dominate the chart.
 */
object SmoothedE1rmMetric : MetricProvider {
    override val key = MetricKeys.SmoothedE1rmKg

    override fun calculate(points: List<WorkoutDataPoint>): MetricValue {
        val perSession = points
            .filter { it.strengthSet != null && it.strengthSet.isWarmup != true }
            .groupBy { it.session.id }
            .values
            .mapNotNull { sessionPoints ->
                val estimate = sessionPoints.maxOfOrNull { point ->
                    point.strengthSet?.let { Estimates.epley(it.weightKg, it.reps) } ?: 0.0
                } ?: return@mapNotNull null
                sessionPoints.first().session.startedAtUtc to estimate
            }
            .sortedBy { it.first }

        var smoothed: Double? = null
        perSession.forEach { (_, estimate) ->
            smoothed = smoothed?.let { previous -> (0.35 * estimate) + (0.65 * previous) } ?: estimate
        }
        return MetricValue(smoothed ?: 0.0, MetricUnit.Kilograms)
    }
}

object BuiltInDimensions {
    val all: List<DimensionProvider> = listOf(
        dimension(DimensionKeys.Exercise) { point, _ -> point.entry?.exerciseSnapshotName.orEmpty() },
        dimension(DimensionKeys.ExerciseId) { point, _ -> point.entry?.exerciseId.orEmpty() },
        dimension(DimensionKeys.BodyPart) { point, _ ->
            point.entry?.exerciseSnapshotPrimaryBodyPart.orEmpty()
        },
        dimension(DimensionKeys.Category) { point, _ -> point.entry?.entryType?.name.orEmpty() },
        dimension(DimensionKeys.Session) { point, _ -> point.session.name },
        dimension(DimensionKeys.SessionId) { point, _ -> point.session.id },
        dimension(DimensionKeys.Status) { point, _ -> point.session.status.name },
        dimension(DimensionKeys.SetType) { point, _ -> point.strengthSet?.setType?.name.orEmpty() },
        dimension(DimensionKeys.Day) { point, zone ->
            Instant.ofEpochMilli(point.session.startedAtUtc).atZone(zone).toLocalDate().toString()
        },
        dimension(DimensionKeys.Week) { point, zone ->
            val date = Instant.ofEpochMilli(point.session.startedAtUtc).atZone(zone).toLocalDate()
            val week = WeekFields.ISO.weekOfWeekBasedYear()
            val year = WeekFields.ISO.weekBasedYear()
            "%04d-W%02d".format(Locale.ROOT, date.get(year), date.get(week))
        },
        dimension(DimensionKeys.Month) { point, zone ->
            val date = Instant.ofEpochMilli(point.session.startedAtUtc).atZone(zone)
            "%04d-%02d".format(Locale.ROOT, date.year, date.monthValue)
        },
        dimension(DimensionKeys.Year) { point, zone ->
            Instant.ofEpochMilli(point.session.startedAtUtc).atZone(zone).year.toString()
        },
    )

    private fun dimension(
        key: String,
        block: (WorkoutDataPoint, ZoneId) -> String,
    ): DimensionProvider = object : DimensionProvider {
        override val key = key
        override fun resolve(point: WorkoutDataPoint, zoneId: ZoneId): String = block(point, zoneId)
    }
}

private fun Iterable<Double>.averageOrZero(): Double {
    var count = 0
    var sum = 0.0
    forEach {
        count++
        sum += it
    }
    return if (count == 0) 0.0 else sum / count
}
