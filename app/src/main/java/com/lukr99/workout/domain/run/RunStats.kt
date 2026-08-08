package com.lukr99.workout.domain.run

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToLong

/**
 * Pure running analytics — the running counterpart to strength's `RecordsEngine`/`StatsEngine`. No
 * Android, no I/O: it aggregates a list of [Run]s (and, for best-effort PRs, their loaded traces)
 * into the numbers the **Progress** running section shows. Fully unit-tested; distances are metres,
 * durations seconds, pace **seconds per kilometre**.
 *
 * Callers pass a [ZoneId] and a reference "now" (epoch-millis) so week/month bucketing is
 * deterministic and testable; the UI passes `ZoneId.systemDefault()` + `System.currentTimeMillis()`.
 */
object RunStats {

    /** Only runs at least this long count toward best-average-pace (keeps tiny runs from winning). */
    private const val MIN_PACE_PR_METERS = 1_000.0

    /** All-time (or over the supplied subset) roll-up. Pace is distance-weighted across runs. */
    data class Totals(
        val runCount: Int,
        val distanceMeters: Double,
        val movingSeconds: Long,
        val elevationGainM: Double,
        val avgPaceSecPerKm: Double,
    )

    /** One week/month bucket for the distance bars. [startUtc] is the bucket's local start, epoch-ms. */
    data class PeriodBucket(
        val startUtc: Long,
        val distanceMeters: Double,
        val runCount: Int,
        val movingSeconds: Long,
    )

    /** A run's fastest continuous window over a target distance (e.g. best 5 km within a longer run). */
    data class BestEffort(
        val runId: String,
        val startedAtUtc: Long,
        val distanceMeters: Double,
        val durationSeconds: Long,
        val paceSecPerKm: Double,
    )

    /** A whole-run record (longest, most elevation, best average pace). */
    data class RunRecord(
        val runId: String,
        val startedAtUtc: Long,
        val value: Double,
    )

    data class PersonalRecords(
        val fastest1k: BestEffort? = null,
        val fastest5k: BestEffort? = null,
        val fastest10k: BestEffort? = null,
        val fastestHalf: BestEffort? = null,
        val longestRun: RunRecord? = null,
        val mostElevation: RunRecord? = null,
        val bestAvgPace: RunRecord? = null,
    )

    fun totals(runs: List<Run>): Totals {
        if (runs.isEmpty()) return Totals(0, 0.0, 0, 0.0, 0.0)
        val distance = runs.sumOf { it.distanceMeters }
        val moving = runs.sumOf { it.movingSeconds }
        val elevation = runs.sumOf { it.elevationGainM }
        return Totals(
            runCount = runs.size,
            distanceMeters = distance,
            movingSeconds = moving,
            elevationGainM = elevation,
            avgPaceSecPerKm = Pace.paceSecPerKm(distance, moving.toDouble()),
        )
    }

    /**
     * Distance per week for the last [weeks] weeks (Monday-start, local), oldest→newest. Empty weeks
     * are included with zero so a bar chart has a continuous axis.
     */
    fun weeklyDistance(
        runs: List<Run>,
        weeks: Int = 12,
        zone: ZoneId = ZoneId.systemDefault(),
        nowUtc: Long = System.currentTimeMillis(),
    ): List<PeriodBucket> {
        require(weeks > 0)
        val thisWeekStart = Instant.ofEpochMilli(nowUtc).atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val bucketStarts = (weeks - 1 downTo 0).map { thisWeekStart.minusWeeks(it.toLong()) }
        return bucketStarts.map { weekStart ->
            val start = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val inWeek = runs.filter { it.startedAtUtc in start until end }
            PeriodBucket(
                startUtc = start,
                distanceMeters = inWeek.sumOf { it.distanceMeters },
                runCount = inWeek.size,
                movingSeconds = inWeek.sumOf { it.movingSeconds },
            )
        }
    }

    /** Distance per calendar month for the last [months] months (local), oldest→newest. */
    fun monthlyDistance(
        runs: List<Run>,
        months: Int = 6,
        zone: ZoneId = ZoneId.systemDefault(),
        nowUtc: Long = System.currentTimeMillis(),
    ): List<PeriodBucket> {
        require(months > 0)
        val thisMonth = Instant.ofEpochMilli(nowUtc).atZone(zone).toLocalDate()
            .with(TemporalAdjusters.firstDayOfMonth())
        val bucketStarts = (months - 1 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
        return bucketStarts.map { monthStart ->
            val start = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = monthStart.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val inMonth = runs.filter { it.startedAtUtc in start until end }
            PeriodBucket(
                startUtc = start,
                distanceMeters = inMonth.sumOf { it.distanceMeters },
                runCount = inMonth.size,
                movingSeconds = inMonth.sumOf { it.movingSeconds },
            )
        }
    }

    /** Chronological (oldest→newest) average pace per run with positive distance — for a trend line. */
    fun paceTrend(runs: List<Run>): List<Pair<Long, Double>> =
        runs.asSequence()
            .filter { it.distanceMeters > 0 && it.avgPaceSecPerKm > 0 }
            .sortedBy { it.startedAtUtc }
            .map { it.startedAtUtc to it.avgPaceSecPerKm }
            .toList()

    /** Number of consecutive weeks ending with the current week that have at least one run. */
    fun currentStreakWeeks(
        runs: List<Run>,
        zone: ZoneId = ZoneId.systemDefault(),
        nowUtc: Long = System.currentTimeMillis(),
    ): Int {
        if (runs.isEmpty()) return 0
        val activeWeeks = runs.map {
            Instant.ofEpochMilli(it.startedAtUtc).atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.toHashSet()
        var week = Instant.ofEpochMilli(nowUtc).atZone(zone).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var streak = 0
        while (week in activeWeeks) {
            streak++
            week = week.minusWeeks(1)
        }
        return streak
    }

    /**
     * Personal records across runs. Best-effort distances (1k/5k/10k/half) need each run's loaded
     * [Run.trace]; whole-run records (longest, most elevation, best average pace) use the summaries.
     */
    fun personalRecords(runs: List<Run>): PersonalRecords {
        fun bestEffort(target: Double): BestEffort? =
            runs.mapNotNull { run ->
                fastestTimeForDistanceMs(run.trace, target)?.let { ms ->
                    val sec = (ms / 1000.0)
                    BestEffort(run.id, run.startedAtUtc, target, sec.roundToLong(), Pace.paceSecPerKm(target, sec))
                }
            }.minByOrNull { it.paceSecPerKm }

        val longest = runs.filter { it.distanceMeters > 0 }
            .maxByOrNull { it.distanceMeters }
            ?.let { RunRecord(it.id, it.startedAtUtc, it.distanceMeters) }
        val elevation = runs.filter { it.elevationGainM > 0 }
            .maxByOrNull { it.elevationGainM }
            ?.let { RunRecord(it.id, it.startedAtUtc, it.elevationGainM) }
        val bestPace = runs.filter { it.distanceMeters >= MIN_PACE_PR_METERS && it.avgPaceSecPerKm > 0 }
            .minByOrNull { it.avgPaceSecPerKm }
            ?.let { RunRecord(it.id, it.startedAtUtc, it.avgPaceSecPerKm) }

        return PersonalRecords(
            fastest1k = bestEffort(Pace.METERS_PER_KM),
            fastest5k = bestEffort(5 * Pace.METERS_PER_KM),
            fastest10k = bestEffort(10 * Pace.METERS_PER_KM),
            fastestHalf = bestEffort(HALF_MARATHON_METERS),
            longestRun = longest,
            mostElevation = elevation,
            bestAvgPace = bestPace,
        )
    }

    /**
     * Minimum time (ms) to cover [target] continuous metres anywhere in [points], or null if the trace
     * is shorter than [target]. A forward two-pointer over cumulative distance with linear time
     * interpolation at the window's trailing edge, so the window measures exactly [target] metres.
     */
    fun fastestTimeForDistanceMs(points: List<TracePoint>, target: Double): Double? {
        val n = points.size
        if (n < 2 || target <= 0) return null
        val cum = DoubleArray(n)
        for (i in 1 until n) {
            cum[i] = cum[i - 1] + Pace.haversineMeters(
                points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon,
            )
        }
        if (cum[n - 1] < target) return null

        var best = Double.MAX_VALUE
        var start = 0
        for (end in 1 until n) {
            val need = cum[end] - target
            if (need < 0) continue
            while (start + 1 < end && cum[start + 1] <= need) start++
            val segStart = cum[start]
            val segEnd = cum[start + 1]
            val frac = if (segEnd > segStart) (need - segStart) / (segEnd - segStart) else 0.0
            val startTime = points[start].t + frac * (points[start + 1].t - points[start].t)
            val windowMs = points[end].t - startTime
            if (windowMs in 0.0..best) best = windowMs
        }
        return best.takeIf { it != Double.MAX_VALUE }
    }

    const val HALF_MARATHON_METERS = 21_097.5
}
