package com.lukr99.workout.domain.stats

import com.lukr99.workout.domain.query.WorkoutDataPoint
import com.lukr99.workout.domain.dateIn
import com.lukr99.workout.domain.isoWeekNumber
import com.lukr99.workout.domain.isoWeekYear
import com.lukr99.workout.domain.padded
import kotlinx.datetime.TimeZone

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
            point.session.startedAtUtc.dateIn(zone).toString()
        },
        dimension(DimensionKeys.Week) { point, zone ->
            val date = point.session.startedAtUtc.dateIn(zone)
            "${padded(date.isoWeekYear, 4)}-W${padded(date.isoWeekNumber, 2)}"
        },
        dimension(DimensionKeys.Month) { point, zone ->
            val date = point.session.startedAtUtc.dateIn(zone)
            "${padded(date.year, 4)}-${padded(date.monthNumber, 2)}"
        },
        dimension(DimensionKeys.Year) { point, zone ->
            point.session.startedAtUtc.dateIn(zone).year.toString()
        },
    )

    private fun dimension(
        key: String,
        block: (WorkoutDataPoint, TimeZone) -> String,
    ): DimensionProvider = object : DimensionProvider {
        override val key = key
        override fun resolve(point: WorkoutDataPoint, zoneId: TimeZone): String = block(point, zoneId)
    }
}
