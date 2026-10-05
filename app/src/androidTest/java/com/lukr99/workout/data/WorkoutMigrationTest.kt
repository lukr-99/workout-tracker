package com.lukr99.workout.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lukr99.workout.data.migrations.WorkoutMigrations
import com.lukr99.workout.data.migrations.Migration0002SessionSource
import com.lukr99.workout.data.migrations.Migration0003ExerciseImageUrl
import com.lukr99.workout.data.migrations.Migration0004ExercisePhotoPath
import com.lukr99.workout.data.migrations.Migration0005RunTables
import com.lukr99.workout.data.migrations.Migration0006RunSegmentBreaks
import com.lukr99.workout.data.migrations.Migration0007LiveLoggingMetadata
import com.lukr99.workout.data.migrations.Migration0008ExerciseGuides
import com.lukr99.workout.data.migrations.Migration0009TemplatePlan
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Migration harness: every released schema remains covered by a real fixture. */
@RunWith(AndroidJUnit4::class)
class WorkoutMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutDb::class.java,
    )

    @After
    fun cleanup() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DatabaseName)
    }

    @Test
    fun migratesSchemaOneFixtureToLatest() {
        helper.createDatabase(DatabaseName, 1).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("fixture", "Fixture Lift", 0, "Back", "[]", "", "", 2, null, 0, 90),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc,
                    durationSeconds, notes, perceivedEffort, bodyweightKg
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("session", null, "Fixture Workout", 1, 1000, 2000, 2000, 1, "", null, null),
            )
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            database.query("SELECT name, defaultRestSeconds FROM exercises WHERE id = 'fixture'", null)
                .use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("Fixture Lift", cursor.getString(0))
                    assertEquals(90, cursor.getInt(1))
                }
            database.query(
                "SELECT source, externalKey FROM sessions WHERE id = 'session'",
                null,
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
                assertEquals(true, cursor.isNull(1))
            }
            database.query(
                "SELECT imageUrl, imageAttribution, localImagePath FROM exercises WHERE id = 'fixture'",
                null,
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesSchemaThreePhotoFieldWithoutChangingExistingArtwork() {
        helper.createDatabase(DatabaseName, 3).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds,
                    imageUrl, imageAttribution
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    "fixture", "Fixture Lift", 0, "Back", "[]", "", "", 2, null, 0, 90,
                    "https://example.test/lift.jpg", "Fixture attribution",
                ),
            )
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            database.query(
                "SELECT imageUrl, imageAttribution, localImagePath FROM exercises WHERE id = 'fixture'",
                null,
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("https://example.test/lift.jpg", cursor.getString(0))
                assertEquals("Fixture attribution", cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesSchemaFourToFiveAddingRunTablesNonDestructively() {
        // A v4 database with existing strength history.
        helper.createDatabase(DatabaseName, 4).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("fixture", "Fixture Lift", 0, "Back", "[]", "", "", 2, null, 0, 90),
            )
            execSQL(
                """
                INSERT INTO sessions (
                    id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc,
                    durationSeconds, notes, perceivedEffort, bodyweightKg, source, externalKey
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("session", null, "Fixture Workout", 1, 1000, 2000, 2000, 1, "", null, null, 0, null),
            )
            close()
        }

        // Validate the migrated schema against the checked-in 5.json (schema identity).
        helper.runMigrationsAndValidate(DatabaseName, 5, true, Migration0005RunTables)

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val support = database.openHelper.writableDatabase
            // Pre-existing strength history is untouched.
            support.query("SELECT name FROM exercises WHERE id = 'fixture'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Fixture Lift", cursor.getString(0))
            }

            // The four new run tables exist and accept rows.
            support.execSQL(
                """
                INSERT INTO runs (
                    id, sessionId, startedAtUtc, durationSeconds, movingSeconds, distanceMeters,
                    avgPaceSecPerKm, elevationGainM, calories, avgHr, source, externalKey,
                    encodedPolyline, routeId, notes
                ) VALUES ('r1', NULL, 1000, 600, 590, 2000.0, 300.0, 12.0, NULL, NULL, 0, NULL, 'abc', NULL, '')
                """.trimIndent(),
            )
            support.execSQL(
                "INSERT INTO run_points (runId, t, lat, lon, elevationM, speedMps, hrBpm, accuracyM) " +
                    "VALUES ('r1', 0, 50.0, 14.0, 200.0, 3.3, 150, 5.0)",
            )
            support.query("SELECT COUNT(*) FROM run_points WHERE runId = 'r1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }

            // Child points cascade with their run (FK ON DELETE CASCADE).
            support.execSQL("DELETE FROM runs WHERE id = 'r1'")
            support.query("SELECT COUNT(*) FROM run_points WHERE runId = 'r1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesSchemaFiveToSixAddingRunPointSegmentBreakColumn() {
        // A v5 database with a run and one trace point (pre-segment-break schema).
        helper.createDatabase(DatabaseName, 5).apply {
            execSQL(
                """
                INSERT INTO runs (
                    id, sessionId, startedAtUtc, durationSeconds, movingSeconds, distanceMeters,
                    avgPaceSecPerKm, elevationGainM, calories, avgHr, source, externalKey,
                    encodedPolyline, routeId, notes
                ) VALUES ('r1', NULL, 1000, 600, 590, 2000.0, 300.0, 12.0, NULL, NULL, 0, NULL, 'abc', NULL, '')
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO run_points (runId, t, lat, lon, elevationM, speedMps, hrBpm, accuracyM) " +
                    "VALUES ('r1', 0, 50.0, 14.0, 200.0, 3.3, 150, 5.0)",
            )
            close()
        }

        // Validate the migrated schema against the checked-in 6.json (schema identity).
        helper.runMigrationsAndValidate(DatabaseName, 6, true, Migration0006RunSegmentBreaks)

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val support = database.openHelper.writableDatabase
            // The pre-existing point survives and defaults to segmentStart = 0 (a connected point).
            support.query("SELECT segmentStart FROM run_points WHERE runId = 'r1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            // A new point can carry the break flag (1 = starts a fresh segment after a manual pause).
            support.execSQL(
                "INSERT INTO run_points (runId, t, lat, lon, elevationM, speedMps, hrBpm, accuracyM, segmentStart) " +
                    "VALUES ('r1', 1000, 50.01, 14.0, NULL, 3.3, NULL, 5.0, 1)",
            )
            support.query(
                "SELECT COUNT(*) FROM run_points WHERE runId = 'r1' AND segmentStart = 1",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesSchemaSixToSevenAddingExerciseLifecycleUnitsAndSetTags() {
        helper.createDatabase(DatabaseName, 6).apply {
            execSQL(
                """
                INSERT INTO sessions (
                    id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc,
                    durationSeconds, notes, perceivedEffort, bodyweightKg, source, externalKey
                ) VALUES ('s7', NULL, 'Migration workout', 1, 1000, 2000, 2000, 1, '', NULL, NULL, 0, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO entries (
                    id, workoutSessionId, exerciseId, exerciseSnapshotName,
                    exerciseSnapshotCategory, exerciseSnapshotPrimaryBodyPart, sortOrder,
                    entryType, notes, supersetGroup
                ) VALUES ('e7', 's7', 'ex7', 'Bench', 0, 'Chest', 0, 0, '', NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO strength_sets (
                    id, workoutEntryId, setNumber, reps, weightKg, rir, rpe, performedAtUtc,
                    notes, isWarmup, isPr, durationSeconds, setType
                ) VALUES ('set7', 'e7', 1, 5, 100.0, NULL, NULL, 1500, '', 0, 0, NULL, 3)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 7, true, Migration0007LiveLoggingMetadata)

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            database.query(
                "SELECT weightUnitOverride, startedAtUtc, completedAtUtc FROM entries WHERE id = 'e7'",
                null,
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(true, cursor.isNull(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
            database.query("SELECT tagsJson, setType FROM strength_sets WHERE id = 'set7'", null)
                .use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("[]", cursor.getString(0))
                    assertEquals(3, cursor.getInt(1))
                }
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesSchemaSevenToEightAddingExerciseGuideFields() {
        helper.createDatabase(DatabaseName, 7).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds,
                    imageUrl, imageAttribution, localImagePath
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("ex8", "Fixture Lift", 0, "Back", "[\"Biceps\"]", "Cable", "Seat on 4", 2, null, 0, 90, null, null, null),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 8, true, Migration0008ExerciseGuides).use { db ->
            db.query(
                "SELECT notes, secondaryBodyPartsJson, instructions, videoUrl FROM exercises WHERE id = 'ex8'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Seat on 4", cursor.getString(0))
                assertEquals("[\"Biceps\"]", cursor.getString(1))
                assertEquals("", cursor.getString(2))
                assertEquals(true, cursor.isNull(3))
            }
        }
    }

    @Test
    fun migratesSchemaEightToNineAddingTemplatePlan() {
        helper.createDatabase(DatabaseName, 8).apply {
            execSQL("INSERT INTO templates (id, name, notes) VALUES ('t9', 'Push', '')")
            execSQL(
                """
                INSERT INTO template_exercises (
                    id, templateId, exerciseId, exerciseName, category, bodyPart, sortOrder, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf("te9", "t9", "ex9", "Bench", 0, "Chest", 0, "Pause"),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 9, true, Migration0009TemplatePlan).use { db ->
            db.query(
                "SELECT notes, targetSets, repsMin, repsMax, restSeconds, supersetGroup FROM template_exercises WHERE id = 'te9'",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("Pause", cursor.getString(0))
                (1..5).forEach { assertEquals(true, cursor.isNull(it)) }
            }
        }
    }

    @Test
    fun migratesSchemaOneToTwoAddingSessionSource() {
        helper.createDatabase(DatabaseName, 1).apply {
            execSQL(
                """
                INSERT INTO sessions (
                    id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc,
                    durationSeconds, notes, perceivedEffort, bodyweightKg
                ) VALUES ('s1', NULL, 'Push', 1, 1000, 2000, 2000, 1, 'kept', 8, 82.5)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 2, true, Migration0002SessionSource).use { db ->
            db.query("SELECT name, notes, perceivedEffort, source, externalKey FROM sessions WHERE id = 's1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Push", cursor.getString(0))
                assertEquals("kept", cursor.getString(1))
                assertEquals(8, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals(true, cursor.isNull(4))
            }
        }
    }

    @Test
    fun migratesSchemaTwoToThreeAddingExerciseImage() {
        helper.createDatabase(DatabaseName, 2).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds
                ) VALUES ('ex2', 'Row', 0, 'Back', '["Biceps"]', 'Cable', 'Seat 4', 2, NULL, 1, 90)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 3, true, Migration0003ExerciseImageUrl).use { db ->
            db.query("SELECT name, notes, isArchived, imageUrl, imageAttribution FROM exercises WHERE id = 'ex2'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("Row", cursor.getString(0))
                assertEquals("Seat 4", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(true, cursor.isNull(3))
                assertEquals(true, cursor.isNull(4))
            }
        }
    }

    @Test
    fun migratesSchemaThreeToFourAddingPhotoPath() {
        helper.createDatabase(DatabaseName, 3).apply {
            execSQL(
                """
                INSERT INTO exercises (
                    id, name, category, primaryBodyPart, secondaryBodyPartsJson,
                    equipment, notes, source, externalSourceId, isArchived, defaultRestSeconds,
                    imageUrl, imageAttribution
                ) VALUES ('ex3', 'Squat', 0, 'Legs', '[]', 'Barbell', '', 0, NULL, 0, NULL,
                    'https://example.test/squat.jpg', 'Fixture')
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(DatabaseName, 4, true, Migration0004ExercisePhotoPath).use { db ->
            db.query("SELECT imageUrl, imageAttribution, localImagePath FROM exercises WHERE id = 'ex3'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("https://example.test/squat.jpg", cursor.getString(0))
                assertEquals("Fixture", cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
            }
        }
    }

    private companion object {
        const val DatabaseName = "exercise-images-migration-test"
    }
}
