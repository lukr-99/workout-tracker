package com.lukr99.workout.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
        ).addMigrations(
            WorkoutDb.MIGRATION_1_2,
            WorkoutDb.MIGRATION_2_3,
            WorkoutDb.MIGRATION_3_4,
            WorkoutDb.MIGRATION_4_5,
        )
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
        ).addMigrations(WorkoutDb.MIGRATION_3_4, WorkoutDb.MIGRATION_4_5)
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
        helper.runMigrationsAndValidate(DatabaseName, 5, true, WorkoutDb.MIGRATION_4_5)

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(
            WorkoutDb.MIGRATION_1_2,
            WorkoutDb.MIGRATION_2_3,
            WorkoutDb.MIGRATION_3_4,
            WorkoutDb.MIGRATION_4_5,
        )
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
        helper.runMigrationsAndValidate(DatabaseName, 6, true, WorkoutDb.MIGRATION_5_6)

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).addMigrations(
            WorkoutDb.MIGRATION_1_2,
            WorkoutDb.MIGRATION_2_3,
            WorkoutDb.MIGRATION_3_4,
            WorkoutDb.MIGRATION_4_5,
            WorkoutDb.MIGRATION_5_6,
        )
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

    private companion object {
        const val DatabaseName = "exercise-images-migration-test"
    }
}
