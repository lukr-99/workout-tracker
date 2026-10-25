package com.lukr99.workout.data.migrations

import com.lukr99.workout.data.RoomWorkoutRepository
import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.RoomTransactionRunner
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.SetTag
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/**
 * The newest migration's predecessor test, in seconds on the JVM (CodePrint testing-and-ci). The
 * version 7 database is built straight from the exported `7.json`, filled with plain SQL, then
 * opened by Room with every migration and read back through the app's own repository. The
 * instrumented WorkoutMigrationTest keeps the full chain on a device.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class NewestMigrationJvmTest {

    @Test
    fun aVersionSevenDatabaseOpensAtEightWithItsDataIntact() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "jvm-migration-7.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, version = 7)
            db.execSQL(
                "INSERT INTO exercises (id, name, category, primaryBodyPart, secondaryBodyPartsJson, equipment, " +
                    "notes, source, externalSourceId, isArchived, defaultRestSeconds, imageUrl, imageAttribution, " +
                    "localImagePath) VALUES ('ex7', 'Bench', 0, 'Chest', '[\"Triceps\"]', 'Barbell', 'Seat 4', 2, " +
                    "NULL, 0, 120, NULL, NULL, NULL)",
            )
            db.execSQL(
                "INSERT INTO sessions (id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc, " +
                    "durationSeconds, notes, perceivedEffort, bodyweightKg, source, externalKey) " +
                    "VALUES ('s7', NULL, 'Push', 1, 1000, 2000, 2000, 1, 'Felt good', NULL, NULL, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO entries (id, workoutSessionId, exerciseId, exerciseSnapshotName, " +
                    "exerciseSnapshotCategory, exerciseSnapshotPrimaryBodyPart, sortOrder, entryType, notes, " +
                    "supersetGroup, weightUnitOverride, startedAtUtc, completedAtUtc) " +
                    "VALUES ('e7', 's7', 'ex7', 'Bench', 0, 'Chest', 0, 0, 'Grip wider', NULL, 1, 1100, 1900)",
            )
            db.execSQL(
                "INSERT INTO strength_sets (id, workoutEntryId, setNumber, reps, weightKg, rir, rpe, " +
                    "performedAtUtc, notes, isWarmup, isPr, durationSeconds, setType, tagsJson) " +
                    "VALUES ('set7', 'e7', 1, 5, 100.0, 1.0, NULL, 1500, '', 0, 1, NULL, 0, '[2]')",
            )
            db.version = 7
        }

        val room = Room.databaseBuilder(context, WorkoutDb::class.java, name)
            .addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = RoomWorkoutRepository(room.workoutDao(), RoomTransactionRunner(room))
            val exercise = repo.getExercise("ex7")!!
            assertEquals("Seat 4", exercise.notes)
            assertEquals(listOf("Triceps"), exercise.secondaryBodyParts)
            assertEquals("", exercise.instructions)
            assertNull(exercise.videoUrl)

            val entry = repo.getSession("s7")!!.entries.single()
            assertEquals("Grip wider", entry.notes)
            val set = entry.strengthSets.single()
            assertEquals(100.0, set.weightKg, 1e-9)
            assertEquals(setOf(SetTag.ToFailure), set.tags)
        } finally {
            room.close()
        }
    }

    @Test
    fun aVersionEightTemplateOpensAtNineWithoutAPlan() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "jvm-migration-8.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, version = 8)
            db.execSQL("INSERT INTO templates (id, name, notes) VALUES ('t8', 'Push', 'Heavy first')")
            db.execSQL(
                "INSERT INTO template_exercises (id, templateId, exerciseId, exerciseName, category, bodyPart, " +
                    "sortOrder, notes) VALUES ('te8', 't8', 'ex8', 'Bench', 0, 'Chest', 0, 'Pause')",
            )
            db.version = 8
        }

        val room = Room.databaseBuilder(context, WorkoutDb::class.java, name)
            .addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = RoomWorkoutRepository(room.workoutDao(), RoomTransactionRunner(room))
            val template = repo.getTemplate("t8")!!
            assertEquals("Heavy first", template.notes)
            val exercise = template.exercises.single()
            assertEquals("Pause", exercise.notes)
            assertNull(exercise.targetSets)
            assertNull(exercise.repsMin)
            assertNull(exercise.repsMax)
            assertNull(exercise.restSeconds)
            assertNull(exercise.supersetGroup)

            val planned = repo.saveTemplate(template.copy(exercises = listOf(exercise.copy(targetSets = 4, repsMin = 6, repsMax = 8))))
            assertEquals(4, planned.exercises.single().targetSets)
        } finally {
            room.close()
        }
    }

    @Test
    fun aVersionNineDatabaseOpensAtTenWithUnknownLocationsAndSharedExercises() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "jvm-migration-9.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, version = 9)
            db.execSQL(
                "INSERT INTO exercises (id, name, category, primaryBodyPart, secondaryBodyPartsJson, equipment, " +
                    "notes, source, externalSourceId, isArchived, defaultRestSeconds, imageUrl, imageAttribution, " +
                    "localImagePath, instructions, videoUrl) VALUES ('fly', 'Cable Fly', 0, 'Chest', '[]', 'Cable', " +
                    "'', 2, NULL, 0, NULL, NULL, NULL, NULL, '', NULL)",
            )
            db.execSQL(
                "INSERT INTO sessions (id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc, " +
                    "durationSeconds, notes, perceivedEffort, bodyweightKg, source, externalKey) " +
                    "VALUES ('s9', NULL, 'Push', 1, 1000, 2000, 2000, 1, '', NULL, NULL, 0, NULL)",
            )
            db.execSQL(
                "INSERT INTO entries (id, workoutSessionId, exerciseId, exerciseSnapshotName, " +
                    "exerciseSnapshotCategory, exerciseSnapshotPrimaryBodyPart, sortOrder, entryType, notes, " +
                    "supersetGroup, weightUnitOverride, startedAtUtc, completedAtUtc) " +
                    "VALUES ('e9', 's9', 'fly', 'Cable Fly', 0, 'Chest', 0, 0, '', NULL, NULL, NULL, NULL)",
            )
            db.execSQL(
                "INSERT INTO strength_sets (id, workoutEntryId, setNumber, reps, weightKg, rir, rpe, " +
                    "performedAtUtc, notes, isWarmup, isPr, durationSeconds, setType, tagsJson) " +
                    "VALUES ('set9', 'e9', 1, 12, 15.0, NULL, NULL, 1500, '', 0, 0, NULL, 0, '[]')",
            )
            db.version = 9
        }

        val room = Room.databaseBuilder(context, WorkoutDb::class.java, name)
            .addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = RoomWorkoutRepository(room.workoutDao(), RoomTransactionRunner(room))
            assertEquals(listOf("home", "outdoors"), repo.getLocations().map { it.id })
            assertNull(repo.getSession("s9")!!.locationId)
            assertEquals(false, repo.getExercise("fly")!!.trackPerLocation)
            assertEquals(1, repo.countWorkoutsWithoutLocation())

            // The owner names the main gym and gives it the old workouts.
            val gym = repo.saveLocation(com.lukr99.workout.domain.Location(name = "Brno gym"))
            assertEquals(1, repo.assignUnknownWorkoutsTo(gym.id))
            assertEquals(gym.id, repo.getLastUsedLocationId())
            assertEquals(15.0, repo.getLastSets(listOf("fly"), gym.id).getValue("fly").single().weightKg, 1e-9)
        } finally {
            room.close()
        }
    }

    @Test
    fun aVersionTenDatabaseOpensAtElevenWithItsDataAndNoNotes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "jvm-migration-10.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, version = 10)
            db.execSQL("INSERT INTO locations (id, name, kind, latitude, longitude, isArchived) VALUES ('brno', 'Brno gym', 0, NULL, NULL, 0)")
            db.execSQL(
                "INSERT INTO exercises (id, name, category, primaryBodyPart, secondaryBodyPartsJson, equipment, " +
                    "notes, source, externalSourceId, isArchived, defaultRestSeconds, imageUrl, imageAttribution, " +
                    "localImagePath, instructions, videoUrl, trackPerLocation, splitPromptMuted) VALUES " +
                    "('fly', 'Cable Fly', 0, 'Chest', '[]', 'Cable', 'Slow', 2, NULL, 0, NULL, NULL, NULL, NULL, '', NULL, 1, 0)",
            )
            db.execSQL(
                "INSERT INTO sessions (id, templateId, name, status, startedAtUtc, endedAtUtc, completedDateUtc, " +
                    "durationSeconds, notes, perceivedEffort, bodyweightKg, source, externalKey, locationId) " +
                    "VALUES ('s10', NULL, 'Push', 1, 1000, 2000, 2000, 1, '', NULL, NULL, 0, NULL, 'brno')",
            )
            db.version = 10
        }

        val room = Room.databaseBuilder(context, WorkoutDb::class.java, name)
            .addMigrations(*WorkoutMigrations.ALL)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = RoomWorkoutRepository(room.workoutDao(), RoomTransactionRunner(room))
            // Nothing that was there changed.
            assertEquals("brno", repo.getSession("s10")!!.locationId)
            assertEquals(true, repo.getExercise("fly")!!.trackPerLocation)
            assertEquals("Slow", repo.getExercise("fly")!!.notes)
            assertEquals(listOf("brno"), repo.getLocations().map { it.id })

            // The new table starts empty and works for the first note.
            assertEquals(emptyList<Any>(), repo.getAllLocationNotes())
            repo.saveLocationNote("fly", "brno", "Double pulley, seat 4")
            assertEquals("Double pulley, seat 4", repo.getLocationNotes(listOf("fly"), "brno").getValue("fly"))
        } finally {
            room.close()
        }
    }

    /** Builds the schema exactly as Room exported it for [version]. */
    private fun createSchema(db: SQLiteDatabase, version: Int) {
        val schemaFile = File("schemas/com.lukr99.workout.data.WorkoutDb/$version.json")
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        for (element in schema.getValue("entities").jsonArray) {
            val entity = element.jsonObject
            val table = entity.text("tableName")
            db.execSQL(entity.text("createSql").replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { index ->
                db.execSQL(index.jsonObject.text("createSql").replace("\${TABLE_NAME}", table))
            }
        }
        schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
    }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
