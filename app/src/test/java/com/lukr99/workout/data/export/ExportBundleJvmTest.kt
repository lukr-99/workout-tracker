package com.lukr99.workout.data.export

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The cross-device contract: our `1.4` bundle round-trips through JSON unchanged, and a hand-crafted
 * `v1.0` export (enums as ints, ISO-8601 timestamps, MAUI computed fields) imports 1:1.
 */
class ExportBundleJvmTest {

    private val bundle = ExportBundle(
        exportedAtUtc = "2024-01-01T10:00:00Z",
        exportFormatVersion = "1.4",
        exercises = listOf(
            Exercise(
                id = "ex1", name = "Bench", category = ExerciseCategory.Strength,
                primaryBodyPart = "Chest", secondaryBodyParts = listOf("Shoulders", "Triceps"),
                equipment = "Barbell", source = ExerciseSource.Seeded, defaultRestSeconds = 120,
                imageUrl = "https://wger.de/media/bench.png",
                localImagePath = "/data/user/0/com.lukr99.workout/files/exercise_images/ex1.jpg",
                imageAttribution = "wger · CC-BY-SA 4",
            ),
        ),
        templates = listOf(
            WorkoutTemplate(
                id = "t1", name = "Push",
                exercises = listOf(WorkoutTemplateExercise(id = "te1", exerciseId = "ex1", exerciseName = "Bench", bodyPart = "Chest")),
            ),
        ),
        sessions = listOf(
            WorkoutSession(
                id = "s1", name = "Push A",
                startedAtUtc = 1_700_000_000_000L, endedAtUtc = 1_700_003_600_000L,
                completedDateUtc = 1_700_003_600_000L, durationSeconds = 3600,
                status = WorkoutSessionStatus.Completed, perceivedEffort = 8, bodyweightKg = 82.5,
                source = WorkoutSessionSource.HealthConnect, externalKey = "health-connect:record",
                entries = listOf(
                    WorkoutEntry(
                        id = "e1", workoutSessionId = "s1", exerciseId = "ex1",
                        exerciseSnapshotName = "Bench", exerciseSnapshotPrimaryBodyPart = "Chest",
                        entryType = ExerciseCategory.Strength, supersetGroup = 1,
                        strengthSets = listOf(
                            StrengthSet(id = "set1", workoutEntryId = "e1", setNumber = 1, reps = 5, weightKg = 100.0, isWarmup = true, setType = SetType.Warmup),
                            StrengthSet(id = "set2", workoutEntryId = "e1", setNumber = 2, reps = 3, weightKg = 110.0, rir = 1.0, isPr = true, durationSeconds = null),
                        ),
                    ),
                    WorkoutEntry(
                        id = "e2", workoutSessionId = "s1", exerciseSnapshotName = "Bike",
                        entryType = ExerciseCategory.Cardio,
                        cardioData = CardioEntryData(workoutEntryId = "e2", durationSeconds = 1200, distanceKm = 6.4),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun roundTrip_isLossless() {
        val json = JsonExporter.toJson(bundle)
        val restored = JsonExporter.fromJson(json)
        assertEquals(bundle, restored)
    }

    @Test
    fun enumsSerializeAsOrdinalsAndTimestampsAsIso() {
        val json = JsonExporter.toJson(bundle)
        assertTrue("category should be the int ordinal", json.contains("\"category\": 0"))
        assertTrue("status should be the int ordinal", json.contains("\"status\": 1"))
        assertTrue("setType Warmup should be ordinal 1", json.contains("\"setType\": 1"))
        assertTrue("session source should be ordinal 1", json.contains("\"source\": 1"))
        assertTrue("timestamps should be ISO strings", json.contains("\"startedAtUtc\": \"2023-11-14"))
    }

    @Test
    fun reads_v10_exportWithComputedFields() {
        val restored = JsonExporter.fromJson(V10_JSON)

        assertEquals("1.0", restored.exportFormatVersion)
        val exercise = restored.exercises.single()
        assertEquals(ExerciseCategory.Strength, exercise.category)
        assertEquals(ExerciseSource.Seeded, exercise.source)
        assertEquals(listOf("Shoulders", "Triceps"), exercise.secondaryBodyParts)
        assertEquals(null, exercise.defaultRestSeconds) // additive field defaults on a 1.0 read
        assertEquals(null, exercise.imageUrl)
        assertEquals(null, exercise.imageAttribution)
        assertEquals(null, exercise.localImagePath)

        val session = restored.sessions.single()
        assertEquals(WorkoutSessionStatus.Completed, session.status)
        assertEquals(WorkoutSessionSource.Local, session.source)
        assertEquals(null, session.externalKey)
        assertEquals(Instant.parse("2024-01-01T09:00:00Z").toEpochMilli(), session.startedAtUtc)
        val set = session.entries.single().strengthSets.single()
        assertEquals(100.0, set.weightKg, 1e-9)
        assertEquals(SetType.Normal, set.setType) // additive default
    }

    @Test
    fun reads_v11_exportWithV12SessionDefaults() {
        val restored = JsonExporter.fromJson(
            V10_JSON.replace(
                "\"exportFormatVersion\": \"1.0\"",
                "\"exportFormatVersion\": \"1.1\"",
            ),
        )

        assertEquals("1.1", restored.exportFormatVersion)
        assertEquals(WorkoutSessionSource.Local, restored.sessions.single().source)
        assertEquals(null, restored.sessions.single().externalKey)
    }

    @Test
    fun supportsAllPublishedVersions() {
        assertEquals(setOf("1.0", "1.1", "1.2", "1.3", "1.4"), ExportBundle.SUPPORTED_VERSIONS)
        assertEquals("1.4", ExportBundle.CURRENT_VERSION)
    }

    private companion object {
        // Mirrors the MAUI System.Text.Json (Web) writer: camelCase, int enums, ISO datetimes, and
        // the get-only computed properties (bodyPartsSummary / isStrength) that a reader must ignore.
        val V10_JSON = """
        {
          "exportedAtUtc": "2024-01-01T10:00:00Z",
          "exportFormatVersion": "1.0",
          "exercises": [
            {
              "id": "abc", "name": "Bench", "category": 0, "primaryBodyPart": "Chest",
              "secondaryBodyParts": ["Shoulders", "Triceps"], "equipment": "Barbell",
              "notes": "", "source": 0, "externalSourceId": null, "isArchived": false,
              "bodyPartsSummary": "Chest / Shoulders / Triceps"
            }
          ],
          "templates": [],
          "sessions": [
            {
              "id": "s1", "templateId": null, "name": "Push",
              "startedAtUtc": "2024-01-01T09:00:00Z", "endedAtUtc": "2024-01-01T10:00:00Z",
              "completedDateUtc": "2024-01-01T10:00:00Z", "durationSeconds": 3600,
              "notes": "", "status": 1,
              "entries": [
                {
                  "id": "e1", "workoutSessionId": "s1", "exerciseId": "abc",
                  "exerciseSnapshotName": "Bench", "exerciseSnapshotCategory": 0,
                  "exerciseSnapshotPrimaryBodyPart": "Chest", "sortOrder": 0, "entryType": 0,
                  "notes": "", "isStrength": true,
                  "strengthSets": [
                    {
                      "id": "set1", "workoutEntryId": "e1", "setNumber": 1, "reps": 5,
                      "weightKg": 100.0, "rir": null, "rpe": null, "performedAtUtc": null, "notes": ""
                    }
                  ],
                  "cardioData": null
                }
              ]
            }
          ]
        }
        """.trimIndent()
    }
}
