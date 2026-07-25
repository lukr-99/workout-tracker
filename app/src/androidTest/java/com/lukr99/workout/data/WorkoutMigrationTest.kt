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

/** Baseline harness: future schema versions add migrations to this class and keep v1 covered. */
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
    fun schemaOneFixtureOpensAndValidatesAgainstExportedSchema() {
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
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDb::class.java,
            DatabaseName,
        ).allowMainThreadQueries().build()
        try {
            database.query("SELECT name, defaultRestSeconds FROM exercises WHERE id = 'fixture'", null)
                .use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("Fixture Lift", cursor.getString(0))
                    assertEquals(90, cursor.getInt(1))
                }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val DatabaseName = "phase-3-5-migration-test"
    }
}
