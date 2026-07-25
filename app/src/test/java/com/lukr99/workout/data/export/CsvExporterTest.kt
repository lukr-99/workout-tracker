package com.lukr99.workout.data.export

import com.lukr99.workout.data.transfer.CsvColumn
import com.lukr99.workout.data.transfer.CsvExportOptions
import com.lukr99.workout.data.transfer.CsvLineEnding
import com.lukr99.workout.data.transfer.WeightUnit
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
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
                ),
            ),
        )

        val lines = artifact.text.lines()
        assertEquals("Session,Exercise,Reps,Weight (lb)", lines[0])
        assertTrue(lines[1].startsWith("\"Push, pull\",\"Bench \"\"A\"\"\",5,"))
        assertTrue(lines[1].endsWith("220.462262"))
        assertEquals(1, artifact.recordCount)
    }
}
