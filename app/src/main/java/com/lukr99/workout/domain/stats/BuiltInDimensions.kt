package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

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
