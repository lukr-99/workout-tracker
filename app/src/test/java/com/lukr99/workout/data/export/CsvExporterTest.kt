package com.lukr99.workout.data.export

import com.lukr99.workout.data.transfer.CsvColumn
import com.lukr99.workout.data.transfer.CsvExportOptions
import com.lukr99.workout.data.transfer.CsvLineEnding
import com.lukr99.workout.data.transfer.WeightUnit
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {
    @Test
    fun configurableColumnsUnitsDelimiterAndEscaping() {
        val entryId = "entry"
        val session = WorkoutSession(
            id = "session",
            name = "Push, pull",
            startedAtUtc = 1_704_103_200_000L,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    id = entryId,
                    workoutSessionId = "session",
                    exerciseSnapshotName = "Bench \"A\"",
                    entryType = ExerciseCategory.Strength,
                    strengthSets = listOf(
                        StrengthSet(
                            workoutEntryId = entryId,
                            setNumber = 1,
                            reps = 5,
                            weightKg = 100.0,
                            tags = setOf(SetTag.ToFailure, SetTag.Failed),
                        ),
                    ),
                ),
            ),
        )
        val artifact = CsvExporter.export(
            listOf(session),
            CsvExportOptions(
                weightUnit = WeightUnit.Pounds,
                timeZoneId = "UTC",
                delimiter = ',',
                lineEnding = CsvLineEnding.Lf,
                columns = listOf(
                    CsvColumn.SessionName,
                    CsvColumn.ExerciseName,
                    CsvColumn.Reps,
                    CsvColumn.Weight,
                    CsvColumn.SetTags,
                ),
            ),
        )

        val lines = artifact.text.lines()
        assertEquals("Session,Exercise,Reps,Weight (lb),Set Tags", lines[0])
        assertTrue(lines[1].startsWith("\"Push, pull\",\"Bench \"\"A\"\"\",5,"))
        assertTrue(lines[1].endsWith("220.462262,ToFailure|Failed"))
        assertEquals(1, artifact.recordCount)
    }

    private fun exportDates(zoneId: String, startedAtUtc: Long, completedAtUtc: Long?): List<String> {
        val session = WorkoutSession(
            id = "s",
            name = "Day",
            startedAtUtc = startedAtUtc,
            completedDateUtc = completedAtUtc,
            status = WorkoutSessionStatus.Completed,
            entries = listOf(
                WorkoutEntry(
                    id = "e",
                    workoutSessionId = "s",
                    exerciseSnapshotName = "Row",
                    entryType = ExerciseCategory.Strength,
                    strengthSets = listOf(StrengthSet(workoutEntryId = "e", setNumber = 1, reps = 5)),
                ),
            ),
        )
        val artifact = CsvExporter.export(
            listOf(session),
            CsvExportOptions(
                timeZoneId = zoneId,
                lineEnding = CsvLineEnding.Lf,
                columns = listOf(CsvColumn.StartedAt, CsvColumn.CompletedAt),
            ),
        )
        return artifact.text.lines()
    }

    @Test
    fun datesAreIsoOffsetTimestampsWithTheOffsetOfTheChosenZone() {
        assertEquals(
            listOf("Started At,Completed At", "2024-01-01T11:00:00+01:00,2024-01-01T12:30:15.5+01:00"),
            exportDates("Europe/Prague", 1_704_103_200_000L, 1_704_108_615_500L),
        )
        assertEquals(
            listOf("Started At,Completed At", "2024-01-01T10:00:00Z,"),
            exportDates("UTC", 1_704_103_200_000L, null),
        )
    }

    @Test
    fun dateFormatMatchesTheJavaTimeIsoOffsetFormatter() {
        val zones = listOf("UTC", "Europe/Prague", "America/New_York", "Asia/Kolkata", "Pacific/Chatham", "-03:30")
        val instants = listOf(
            0L, 1L, 999L, 1_000L, 120L, 1_704_103_200_000L, 1_711_846_800_123L, 1_729_989_000_450L,
            -1L, -62_135_596_800_000L + 86_400_000L, 4_102_444_799_999L,
        )
        for (zoneId in zones) {
            for (millis in instants) {
                val expected = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)))
                assertEquals("$millis in $zoneId", listOf("Started At,Completed At", "$expected,"), exportDates(zoneId, millis, null))
            }
        }
    }
}
