package com.lukr99.workout.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Room coverage mirroring the MAUI `WorkoutTracker.Tests`: seed-on-empty, the catalog
 * filter, archive-not-delete, template → session snapshotting, cascade deletes, and a bundle
 * export → JSON → import round-trip into a fresh database.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var db: WorkoutDb
    private lateinit var repo: WorkoutRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WorkoutRepository(db.workoutDao(), RoomTransactionRunner(db))
    }

    @After
    fun teardown() = db.close()

    private fun rowCount(table: String): Int =
        db.query(SimpleSQLiteQuery("SELECT COUNT(*) FROM $table")).use { c ->
            c.moveToFirst(); c.getInt(0)
        }

    @Test
    fun seed_populatesSixteenExercisesAndIsIdempotent() = runTest {
        repo.ensureSeeded()
        assertEquals(16, repo.getExercises(ExerciseFilter(includeArchived = true)).size)
        repo.ensureSeeded() // second run must not duplicate
        assertEquals(16, repo.getExercises(ExerciseFilter(includeArchived = true)).size)
    }

    @Test
    fun exerciseFilter_matchesMauiBehaviour() = runTest {
        repo.ensureSeeded()

        assertEquals(5, repo.getExercises(ExerciseFilter(category = ExerciseCategory.Cardio)).size)
        assertEquals(
            "Barbell Bench Press",
            repo.getExercises(ExerciseFilter(searchText = "bench")).single().name,
        )
        assertEquals(2, repo.getExercises(ExerciseFilter(bodyPart = "Chest")).size) // Bench + Incline
        assertTrue(repo.getExercises(ExerciseFilter(equipment = "Barbell")).isNotEmpty())

        // Ordering: Strength (category 0) before Cardio (category 1).
        val all = repo.getExercises(ExerciseFilter(includeArchived = true))
        assertEquals(ExerciseCategory.Strength, all.first().category)
        assertEquals(ExerciseCategory.Cardio, all.last().category)
    }

    @Test
    fun archive_hidesFromCatalogButKeepsRow() = runTest {
        val saved = repo.saveExercise(Exercise(name = "Face Pull", primaryBodyPart = "Shoulders"))
        repo.archiveExercise(saved.id)

        assertTrue(repo.getExercises(ExerciseFilter()).none { it.id == saved.id })
        val archived = repo.getExercise(saved.id)
        assertNotNull(archived)
        assertTrue(archived!!.isArchived)
        assertTrue(repo.getExercises(ExerciseFilter(includeArchived = true)).any { it.id == saved.id })
    }

    @Test
    fun externalMergeOnlyEnrichesSyncedRowsAndProtectsUserCatalog() = runTest {
        val custom = repo.saveExercise(
            Exercise(
                id = "custom",
                name = "My Press",
                primaryBodyPart = "Chest",
                source = ExerciseSource.Custom,
                externalSourceId = "wger:protected",
            ),
        )
        val synced = repo.saveExercise(
            Exercise(
                id = "synced",
                name = "Remote Row",
                primaryBodyPart = "Back",
                source = ExerciseSource.Synced,
                externalSourceId = "wger:update",
                notes = "Keep my note",
            ),
        )

        val result = repo.mergeExternalExercisesDetailed(
            listOf(
                custom.copy(name = "Overwrite attempt", notes = "Remote"),
                synced.copy(
                    name = "Remote rename",
                    secondaryBodyParts = listOf("Biceps"),
                    equipment = "Cable",
                    notes = "Overwrite attempt",
                    imageUrl = "https://wger.de/media/remote-row.png",
                    imageAttribution = "wger · CC-BY-SA 4",
                ),
                Exercise(name = "My Press", externalSourceId = "wger:name-collision"),
                Exercise(name = "New Remote", externalSourceId = "wger:new"),
            ),
        )

        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        assertEquals(2, result.skipped)
        assertEquals("My Press", repo.getExercise("custom")!!.name)
        val updated = repo.getExercise("synced")!!
        assertEquals("Remote Row", updated.name)
        assertEquals("Keep my note", updated.notes)
        assertEquals("Cable", updated.equipment)
        assertEquals(listOf("Biceps"), updated.secondaryBodyParts)
        assertEquals("https://wger.de/media/remote-row.png", updated.imageUrl)
        assertEquals("wger · CC-BY-SA 4", updated.imageAttribution)
    }

    @Test
    fun imageBackfillMatchesNormalizedNamesAndOnlyFillsArtwork() = runTest {
        val existing = repo.saveExercise(
            Exercise(
                id = "seeded",
                name = "Back Squat",
                primaryBodyPart = "Legs",
                equipment = "My rack",
                notes = "Keep this",
                source = ExerciseSource.Seeded,
            ),
        )

        val filled = repo.backfillMissingExerciseImages(
            listOf(
                Exercise(
                    name = "Barbell Full Squat",
                    imageUrl = "https://wger.de/media/squat.jpg",
                    imageAttribution = "wger",
                ),
            ),
        )

        assertEquals(1, filled)
        val updated = repo.getExercise(existing.id)!!
        assertEquals("Back Squat", updated.name)
        assertEquals("My rack", updated.equipment)
        assertEquals("Keep this", updated.notes)
        assertEquals("https://wger.de/media/squat.jpg", updated.imageUrl)
        assertEquals("wger", updated.imageAttribution)

        assertEquals(
            0,
            repo.backfillMissingExerciseImages(
                listOf(
                    Exercise(
                        name = "Back Squat",
                        imageUrl = "https://example.test/replacement.jpg",
                    ),
                ),
            ),
        )
        assertEquals("https://wger.de/media/squat.jpg", repo.getExercise(existing.id)!!.imageUrl)
    }

    @Test
    fun templateToSession_snapshotsAndSurvivesCatalogEdit() = runTest {
        val exercise = repo.saveExercise(Exercise(name = "Back Squat", primaryBodyPart = "Legs"))
        val template = repo.saveTemplate(
            WorkoutTemplate(
                name = "Leg Day",
                exercises = listOf(
                    WorkoutTemplateExercise(
                        exerciseId = exercise.id, exerciseName = exercise.name,
                        category = exercise.category, bodyPart = exercise.primaryBodyPart,
                    ),
                ),
            ),
        )

        val session = repo.createWorkoutSession(templateId = template.id)
        val entry = session.entries.single()
        assertEquals("Back Squat", entry.exerciseSnapshotName)
        assertEquals("Legs", entry.exerciseSnapshotPrimaryBodyPart)
        assertEquals(1, entry.strengthSets.size) // seeded first set

        // Renaming the catalog exercise must not rewrite the logged snapshot.
        repo.saveExercise(exercise.copy(name = "High-Bar Squat"))
        val reloaded = repo.getSession(session.id)!!
        assertEquals("Back Squat", reloaded.entries.single().exerciseSnapshotName)
    }

    @Test
    fun createWorkoutSession_returnsExistingActive() = runTest {
        val first = repo.createWorkoutSession(name = "Quick")
        val second = repo.createWorkoutSession(name = "Another")
        assertEquals(first.id, second.id) // only one active session at a time
    }

    @Test
    fun deleteSession_cascadesEntriesSetsAndCardio() = runTest {
        val session = repo.saveWorkoutSession(completedSessionWithChildren())
        assertEquals(1, rowCount("sessions"))
        assertEquals(2, rowCount("entries"))
        assertEquals(2, rowCount("strength_sets"))
        assertEquals(1, rowCount("cardio_data"))

        repo.deleteWorkoutSession(session.id)

        assertEquals(0, rowCount("sessions"))
        assertEquals(0, rowCount("entries"))
        assertEquals(0, rowCount("strength_sets"))
        assertEquals(0, rowCount("cardio_data"))
    }

    @Test
    fun resaveSession_replacesChildrenWithoutOrphans() = runTest {
        val session = repo.saveWorkoutSession(completedSessionWithChildren())
        assertEquals(2, rowCount("strength_sets"))

        // Drop the strength entry; only cardio remains — old sets must be gone.
        val trimmed = session.copy(entries = session.entries.filter { it.entryType == ExerciseCategory.Cardio })
        repo.saveWorkoutSession(trimmed)

        assertEquals(1, rowCount("entries"))
        assertEquals(0, rowCount("strength_sets"))
        assertEquals(1, rowCount("cardio_data"))
    }

    @Test
    fun concurrentSessionSaves_areAtomicAndLeaveAConsistentGraph() = runTest {
        val original = repo.saveWorkoutSession(completedSessionWithChildren())
        val strengthOnly = original.copy(
            entries = original.entries.filter { it.entryType == ExerciseCategory.Strength },
        )
        val cardioOnly = original.copy(
            entries = original.entries.filter { it.entryType == ExerciseCategory.Cardio },
        )

        coroutineScope {
            listOf(strengthOnly, cardioOnly).map { session ->
                async(Dispatchers.Default) { repo.saveWorkoutSession(session) }
            }.awaitAll()
        }

        val saved = repo.getSession(original.id)!!
        val savedIds = saved.entries.map { it.id }.toSet()
        assertTrue(
            savedIds == strengthOnly.entries.map { it.id }.toSet() ||
                savedIds == cardioOnly.entries.map { it.id }.toSet(),
        )
        assertEquals(saved.entries.size, rowCount("entries"))
        assertEquals(saved.entries.sumOf { it.strengthSets.size }, rowCount("strength_sets"))
        assertEquals(saved.entries.count { it.cardioData != null }, rowCount("cardio_data"))
        val orphanedSets = db.query(
            SimpleSQLiteQuery(
                """
                SELECT COUNT(*) FROM strength_sets AS sets
                LEFT JOIN entries ON entries.id = sets.workoutEntryId
                WHERE entries.id IS NULL
                """.trimIndent(),
            ),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals(0, orphanedSets)
    }

    @Test
    fun completedSession_appearsInHistoryWithVolume() = runTest {
        repo.saveWorkoutSession(completedSessionWithChildren())
        val history = repo.getWorkoutHistory()
        assertEquals(1, history.size)
        assertEquals(100.0 * 5 + 110.0 * 3, history.single().totalVolumeKg, 1e-9)
    }

    @Test
    fun exportImport_roundTripsIntoFreshDatabase() = runTest {
        repo.ensureSeeded()
        repo.saveTemplate(WorkoutTemplate(name = "Push", exercises = emptyList()))
        repo.saveWorkoutSession(completedSessionWithChildren())

        val original = repo.createExportBundle()
        val json = JsonExporter.toJson(original)

        val freshDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), WorkoutDb::class.java,
        ).allowMainThreadQueries().build()
        try {
            val freshRepo = WorkoutRepository(freshDb.workoutDao())
            freshRepo.importBundle(JsonExporter.fromJson(json))
            val reExported = freshRepo.createExportBundle()

            // Ignore the export timestamp; assert the content is identical.
            assertEquals(original.copy(exportedAtUtc = ""), reExported.copy(exportedAtUtc = ""))
        } finally {
            freshDb.close()
        }
    }

    @Test
    fun lyftaPreviewCommit_isAtomicAndIdempotent() = runTest {
        repo.ensureSeeded()
        val service = DataTransferService(repo)
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val csv = testContext.assets.open("lyfta-sample.csv").bufferedReader().use { it.readText() }

        val preview = service.previewImport(
            csv,
            "lyfta-sample.csv",
            ImportOptions(sourceTimeZoneId = "Europe/Prague"),
        )
        assertTrue(preview.canCommit)
        assertEquals(2, preview.summary.insertedSessions)
        assertEquals(1, preview.summary.insertedExercises)

        val committed = service.commitImport(preview)
        assertEquals(2, committed.insertedSessions)
        assertEquals(17, repo.getExercises(ExerciseFilter(includeArchived = true)).size)
        assertEquals(2, repo.getSessions().size)

        val secondPreview = service.previewImport(
            csv,
            "lyfta-sample.csv",
            ImportOptions(sourceTimeZoneId = "Europe/Prague"),
        )
        assertEquals(2, secondPreview.summary.skippedSessions)
        val secondCommit = service.commitImport(secondPreview)
        assertEquals(2, secondCommit.skippedSessions)
        assertEquals(2, repo.getSessions().size)
    }

    @Test
    fun freshDatabase_hasNoActiveSession() = runTest {
        assertNull(repo.getActiveSession())
        assertFalse(repo.getExercises(ExerciseFilter(includeArchived = true)).isNotEmpty())
    }

    private fun completedSessionWithChildren() = WorkoutSession(
        name = "Push A",
        startedAtUtc = 1_700_000_000_000L,
        endedAtUtc = 1_700_003_600_000L,
        completedDateUtc = 1_700_003_600_000L,
        durationSeconds = 3600,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                exerciseId = "bench", exerciseSnapshotName = "Bench",
                exerciseSnapshotPrimaryBodyPart = "Chest", entryType = ExerciseCategory.Strength,
                strengthSets = listOf(
                    StrengthSet(reps = 5, weightKg = 100.0),
                    StrengthSet(reps = 3, weightKg = 110.0),
                ),
            ),
            WorkoutEntry(
                exerciseSnapshotName = "Bike", entryType = ExerciseCategory.Cardio,
                cardioData = CardioEntryData(durationSeconds = 1200, distanceKm = 6.4),
            ),
        ),
    )
}
