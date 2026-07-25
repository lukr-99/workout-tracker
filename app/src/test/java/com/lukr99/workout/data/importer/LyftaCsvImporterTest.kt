package com.lukr99.workout.data.importer

import com.lukr99.workout.data.Seed
import com.lukr99.workout.data.transfer.ExerciseMatchMode
import com.lukr99.workout.data.transfer.ImportContext
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.SetType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyftaCsvImporterTest {
    private val catalog = Seed.exercises()
    private val csv = checkNotNull(javaClass.classLoader?.getResource("lyfta-sample.csv"))
        .readText()

    @Test
    fun parsesRealSchema_setTypesTimedSetsSupersetsAndCardio() {
        val payload = LyftaCsvImporter.parse(
            csv,
            ImportContext(catalog, emptyList(), emptyList()),
            ImportOptions(
                sourceTimeZoneId = "Europe/Prague",
                exerciseMatchMode = ExerciseMatchMode.Aliases,
            ),
            "lyfta-sample.csv",
        )

        assertTrue(payload.issues.none { it.severity.name == "Error" })
        assertEquals(5, payload.sourceRows)
        assertEquals(2, payload.sessions.size)
        val push = payload.sessions.first()
        assertEquals(3, push.entries.size)
        assertEquals(3_930, push.durationSeconds)
        assertEquals(Instant.parse("2026-06-01T06:00:00Z").toEpochMilli(), push.startedAtUtc)
        val bench = push.entries.first { it.exerciseSnapshotName == "Bench Press" }
        assertEquals(
            catalog.first { it.name == "Barbell Bench Press" }.id,
            bench.exerciseId,
        )
        assertEquals(listOf(SetType.Warmup, SetType.Normal), bench.strengthSets.map { it.setType })
        assertTrue(bench.strengthSets.first().isWarmup)
        assertEquals(1, bench.supersetGroup)
        val plank = push.entries.first { it.exerciseSnapshotName == "Plank" }
        assertEquals(75, plank.strengthSets.single().durationSeconds)

        val cardio = payload.sessions.last().entries.single()
        assertEquals(ExerciseCategory.Cardio, cardio.entryType)
        assertEquals(1_800, cardio.cardioData?.durationSeconds)
        assertEquals(5.25, cardio.cardioData?.distanceKm ?: 0.0, 1e-9)
        assertEquals(1, payload.exercises.size) // Cable Fly is the only catalog miss.
    }

    @Test
    fun strictMode_reportsMalformedRows() {
        val malformed = csv + "\nBad,,00:10:00,,X,10,5,null,null,NORMAL_SET"
        val payload = LyftaCsvImporter.parse(
            malformed,
            ImportContext(emptyList(), emptyList(), emptyList()),
            ImportOptions(strict = true),
        )
        assertTrue(payload.issues.any { it.code == "csv.strict_rows" })
    }
}
