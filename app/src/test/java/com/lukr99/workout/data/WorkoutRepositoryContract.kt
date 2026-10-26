package com.lukr99.workout.data

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.data.export.JsonExporter
import com.lukr99.workout.data.export.SettingsSnapshot
import com.lukr99.workout.data.run.RunRepository
import com.lukr99.workout.data.transfer.DataTransferService
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.data.transfer.NoPhotoArchive
import com.lukr99.workout.data.transfer.RestoreMode
import com.lukr99.workout.data.transfer.UserDataEraser
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.ExerciseFilter
import com.lukr99.workout.domain.ExerciseLocationNote
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import com.lukr99.workout.domain.run.Route
import com.lukr99.workout.domain.run.RoutePoint
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * What every [WorkoutRepository] and [RunRepository] must do, written once and run against Room
 * ([RoomWorkoutRepositoryContractTest]) and the web edition's document store
 * ([com.lukr99.workout.data.document.DocumentWorkoutRepositoryContractTest]). The scenarios come
 * from the Room tests: seeding, the catalog, archive-not-delete, templates, sessions, locations,
 * machine notes, last time, the dashboard, transfer merge and replace, and delete all data.
 */
abstract class WorkoutRepositoryContract {

    /** One empty store: a workout repository and a run repository over the same data. */
    data class Store(val repo: WorkoutRepository, val runs: RunRepository)

    /** A new, empty store. Each call is independent of the others. */
    protected abstract fun newStore(): Store

    protected lateinit var repo: WorkoutRepository
    protected lateinit var runs: RunRepository

    @Before
    fun openStore() {
        val store = newStore()
        repo = store.repo
        runs = store.runs
    }

    // --- Catalog -------------------------------------------------------------------------------

    @Test
    fun seedingAddsTheStarterCatalogOnceAndTheBuiltInPlaces() = runBlocking {
        assertNull(repo.getActiveSession())
        assertTrue(repo.getExercises(ExerciseFilter(includeArchived = true)).isEmpty())
        repo.ensureSeeded()
        assertEquals(16, repo.getExercises(ExerciseFilter(includeArchived = true)).size)
        repo.ensureSeeded()
        assertEquals(16, repo.countExercises())
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), repo.getLocations().map { it.id })

        // Seeding again keeps a renamed built-in as it is.
        repo.saveLocation(repo.getLocation(Location.HOME_ID)!!.copy(name = "Garage"))
        repo.ensureSeeded()
        assertEquals("Garage", repo.getLocation(Location.HOME_ID)!!.name)
    }

    @Test
    fun theCatalogFilterAndOrder() = runBlocking {
        repo.ensureSeeded()
        assertEquals(5, repo.getExercises(ExerciseFilter(category = ExerciseCategory.Cardio)).size)
        assertEquals("Barbell Bench Press", repo.getExercises(ExerciseFilter(searchText = "bench")).single().name)
        assertEquals(2, repo.getExercises(ExerciseFilter(bodyPart = "Chest")).size)
        val all = repo.getExercises(ExerciseFilter(includeArchived = true))
        assertEquals(ExerciseCategory.Strength, all.first().category)
        assertEquals(ExerciseCategory.Cardio, all.last().category)
        assertEquals(all, repo.observeExercises(ExerciseFilter(includeArchived = true)).first())
    }

    @Test
    fun archivingHidesAnExerciseButKeepsIt() = runBlocking {
        val saved = repo.saveExercise(Exercise(name = "  Face Pull ", primaryBodyPart = "Shoulders"))
        assertEquals("Face Pull", saved.name)
        repo.archiveExercise(saved.id)
        assertTrue(repo.getExercises(ExerciseFilter()).none { it.id == saved.id })
        assertTrue(repo.getExercise(saved.id)!!.isArchived)
        assertTrue(repo.getExercises(ExerciseFilter(includeArchived = true)).any { it.id == saved.id })
    }

    @Test
    fun anExternalMergeOnlyEnrichesSyncedRows() = runBlocking {
        val custom = repo.saveExercise(
            Exercise(id = "custom", name = "My Press", source = ExerciseSource.Custom, externalSourceId = "wger:protected"),
        )
        val synced = repo.saveExercise(
            Exercise(
                id = "synced", name = "Remote Row", primaryBodyPart = "Back",
                source = ExerciseSource.Synced, externalSourceId = "wger:update", notes = "Keep my note",
            ),
        )
        val result = repo.mergeExternalExercisesDetailed(
            listOf(
                custom.copy(name = "Overwrite attempt"),
                synced.copy(name = "Remote rename", equipment = "Cable", notes = "Overwrite attempt", imageUrl = "https://wger.de/a.png"),
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
        assertEquals("https://wger.de/a.png", updated.imageUrl)
    }

    @Test
    fun imageBackfillOnlyFillsMissingArtwork() = runBlocking {
        val squat = repo.saveExercise(Exercise(id = "seeded", name = "Back Squat", equipment = "My rack", source = ExerciseSource.Seeded))
        assertEquals(1, repo.backfillMissingExerciseImages(listOf(Exercise(name = "Barbell Full Squat", imageUrl = "https://wger.de/s.jpg"))))
        assertEquals("https://wger.de/s.jpg", repo.getExercise(squat.id)!!.imageUrl)
        assertEquals("My rack", repo.getExercise(squat.id)!!.equipment)
        assertEquals(0, repo.backfillMissingExerciseImages(listOf(Exercise(name = "Back Squat", imageUrl = "https://x.test/r.jpg"))))
        assertEquals("https://wger.de/s.jpg", repo.getExercise(squat.id)!!.imageUrl)
    }

    // --- Templates and sessions ------------------------------------------------------------------

    @Test
    fun aWorkoutFromATemplateKeepsItsSnapshotAfterACatalogEdit() = runBlocking {
        val exercise = repo.saveExercise(Exercise(name = "Back Squat", primaryBodyPart = "Legs"))
        val template = repo.saveTemplate(
            WorkoutTemplate(
                name = " Leg Day ",
                exercises = listOf(
                    WorkoutTemplateExercise(
                        id = "", exerciseId = exercise.id, exerciseName = exercise.name, bodyPart = "Legs",
                        sortOrder = 5, targetSets = 50, repsMin = 0, repsMax = 8,
                    ),
                ),
            ),
        )
        assertEquals("Leg Day", template.name)
        val planned = template.exercises.single()
        assertTrue(planned.id.isNotBlank())
        assertEquals(0, planned.sortOrder)
        assertEquals(20, planned.targetSets)
        assertEquals(1, planned.repsMin)
        assertEquals(template, repo.getTemplate(template.id))
        assertEquals(listOf(template), repo.observeTemplates().first())

        val session = repo.createWorkoutSession(templateId = template.id)
        assertEquals("Leg Day", session.name)
        val entry = session.entries.single()
        assertEquals("Back Squat", entry.exerciseSnapshotName)
        assertEquals(20, entry.strengthSets.size)

        repo.saveExercise(exercise.copy(name = "High-Bar Squat"))
        assertEquals("Back Squat", repo.getSession(session.id)!!.entries.single().exerciseSnapshotName)

        repo.deleteTemplate(template.id)
        assertNull(repo.getTemplate(template.id))
        assertEquals(0, repo.countTemplates())
    }

    @Test
    fun onlyOneLiveWorkoutAtATime() = runBlocking {
        val first = repo.createWorkoutSession(name = "Quick")
        val second = repo.createWorkoutSession(name = "Another")
        assertEquals(first.id, second.id)
        assertEquals(first, repo.observeActiveSession().first())
        assertEquals(1, repo.countWorkouts())
    }

    @Test
    fun savingAWorkoutNumbersItsChildrenAndResavingReplacesThem() = runBlocking {
        val saved = repo.saveWorkoutSession(completedWithChildren())
        assertEquals(saved, repo.getSession(saved.id))
        assertEquals(saved, repo.observeSession(saved.id).first())
        val strength = saved.entries.first { it.entryType == ExerciseCategory.Strength }
        assertEquals(listOf(1, 2), strength.strengthSets.map { it.setNumber })
        assertTrue(strength.strengthSets.all { it.workoutEntryId == strength.id && it.id.isNotBlank() })
        assertNull(strength.cardioData)
        val cardio = saved.entries.first { it.entryType == ExerciseCategory.Cardio }
        assertEquals(cardio.id, cardio.cardioData!!.workoutEntryId)
        assertEquals(listOf(0, 1), saved.entries.map { it.sortOrder })
        assertEquals("Exercise", cardio.exerciseSnapshotName)

        val trimmed = repo.saveWorkoutSession(saved.copy(entries = listOf(cardio)))
        assertEquals(listOf(cardio.id), repo.getSession(saved.id)!!.entries.map { it.id })
        assertEquals(trimmed, repo.getSession(saved.id))

        repo.deleteWorkoutSession(saved.id)
        assertNull(repo.getSession(saved.id))
    }

    @Test
    fun aFinishedWorkoutGetsItsTimesAndShowsInHistory() = runBlocking {
        val saved = repo.saveWorkoutSession(
            WorkoutSession(name = "", startedAtUtc = 1_000, endedAtUtc = 61_000, status = WorkoutSessionStatus.Completed),
        )
        assertEquals("Quick Workout", saved.name)
        assertEquals(61_000L, saved.completedDateUtc)
        assertEquals(60L, saved.durationSeconds)

        repo.saveWorkoutSession(completedWithChildren())
        val history = repo.getWorkoutHistory()
        assertEquals(2, history.size)
        assertEquals(100.0 * 5 + 110.0 * 3, history.first().totalVolumeKg, 1e-9)
        assertEquals(history, repo.observeHistory().first())
        assertEquals(1, repo.getWorkoutHistory("push").size)
    }

    @Test
    fun discardedWorkoutsAreLeftOutUnlessAskedFor() = runBlocking {
        repo.saveWorkoutSession(completedWithChildren().copy(id = "kept"))
        repo.saveWorkoutSession(WorkoutSession(id = "gone", status = WorkoutSessionStatus.Discarded, startedAtUtc = 5))
        assertEquals(listOf("kept"), repo.getSessions().map { it.id })
        assertEquals(setOf("kept", "gone"), repo.getSessions(includeDiscarded = true).map { it.id }.toSet())
        assertEquals(1, repo.countWorkouts())
        assertEquals(listOf("kept"), repo.createExportBundle().sessions.map { it.id })
    }

    @Test
    fun eachTemplateGetsItsNewestFinishedWorkout() = runBlocking {
        fun workout(template: String?, at: Long, status: WorkoutSessionStatus = WorkoutSessionStatus.Completed) = WorkoutSession(
            templateId = template,
            startedAtUtc = at - 1_000,
            completedDateUtc = at.takeIf { status == WorkoutSessionStatus.Completed },
            status = status,
            entries = listOf(WorkoutEntry(exerciseId = "bench", strengthSets = listOf(StrengthSet(reps = 5, performedAtUtc = at)))),
        )
        repo.saveWorkoutSession(workout("push", at = 1_000_000))
        repo.saveWorkoutSession(workout("push", at = 3_000_000))
        repo.saveWorkoutSession(workout("legs", at = 2_000_000))
        repo.saveWorkoutSession(workout(null, at = 5_000_000))
        repo.saveWorkoutSession(workout("legs", at = 9_000_000, status = WorkoutSessionStatus.Discarded))
        assertEquals(mapOf("push" to 3_000_000L, "legs" to 2_000_000L), repo.getTemplatesLastDone())
        assertEquals(3_000_000L, repo.getTemplateLastDone("push"))
        assertNull(repo.getTemplateLastDone("none"))
    }

    @Test
    fun aWorkoutBecomesATemplate() = runBlocking {
        val session = repo.saveWorkoutSession(completedWithChildren())
        val template = repo.duplicateWorkoutAsTemplate(session.id, "From push")
        assertEquals("From push", template.name)
        assertEquals(template, repo.getTemplate(template.id))
    }

    @Test
    fun theDashboardSnapshot() = runBlocking {
        repeat(7) { index ->
            repo.saveWorkoutSession(workout("w$index", at = 1_700_000_000_000L + index * 86_400_000L, location = null, kg = 10.0 + index))
        }
        val live = repo.createWorkoutSession(name = "Live")
        val dashboard = repo.getDashboardSnapshot()
        assertEquals(live.id, dashboard.activeWorkout?.id)
        assertEquals(listOf("w6", "w5", "w4", "w3", "w2"), dashboard.recentWorkouts.map { it.id })
        assertEquals(repo.getAnalyticsOverview(), dashboard.analytics)
        assertEquals(repo.getConsistencySnapshot(), dashboard.consistency)
        assertEquals(7, repo.getExerciseProgress(FLY).size)
    }

    // --- Locations and last time -----------------------------------------------------------------

    @Test
    fun aNewWorkoutStartsAtTheLastUsedPlaceUnlessItIsArchived() = runBlocking {
        repo.ensureSeeded()
        assertNull(repo.createWorkoutSession().locationId)
        repo.deleteWorkoutSession(repo.getActiveSession()!!.id)

        val brno = repo.saveLocation(Location(name = "  Brno gym "))
        assertEquals("Brno gym", brno.name)
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = null, kg = 15.0))
        assertEquals(brno.id, repo.getLastUsedLocationId())
        assertEquals(brno.id, repo.createWorkoutSession().locationId)

        repo.setLocationArchived(brno.id, true)
        assertNull(repo.getLastUsedLocationId())
        assertTrue(repo.getLocation(brno.id)!!.isArchived)
        assertEquals(brno.id, repo.getSession("w1")!!.locationId)
        assertEquals(repo.getLocations(), repo.observeLocations().first())
        try {
            repo.saveLocation(Location(name = "  "))
            fail("A blank name must be refused")
        } catch (expected: IllegalArgumentException) {
            assertEquals("A location needs a name.", expected.message)
        }
    }

    @Test
    fun movingAndAssigningWorkouts() = runBlocking {
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        val other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = null, kg = 15.0))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = null, kg = 59.0))
        repo.saveWorkoutSession(workout("w3", at = 3_000, location = null, kg = 15.0))
        assertEquals(3, repo.countWorkoutsWithoutLocation())

        assertEquals(1, repo.moveWorkoutsToLocation(listOf("w2", "w2", ""), other.id))
        assertEquals(0, repo.moveWorkoutsToLocation(emptyList(), other.id))
        assertEquals(2, repo.assignUnknownWorkoutsTo(brno.id))
        assertEquals(0, repo.countWorkoutsWithoutLocation())
        assertEquals(listOf(brno.id, other.id, brno.id), listOf("w1", "w2", "w3").map { repo.getSession(it)!!.locationId })
        assertEquals(1, repo.moveWorkoutsToLocation(listOf("w3"), null))
        assertNull(repo.getSession("w3")!!.locationId)
    }

    @Test
    fun lastTimeInBothModes() = runBlocking {
        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly"))
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        val other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveWorkoutSession(workout("w1", at = 1_000, location = brno.id, kg = 15.0, note = "double pulley"))
        repo.saveWorkoutSession(workout("w2", at = 2_000, location = other.id, kg = 59.0, note = "single pulley"))

        assertEquals(59.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals("single pulley", repo.getPreviousEntryNotes(listOf(FLY), "live", brno.id).getValue(FLY).text)
        assertTrue(repo.getPreviousEntryNotes(listOf(FLY), "w2", brno.id).getValue(FLY).text == "double pulley")

        repo.setTrackPerLocation(FLY, true)
        assertEquals(15.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals("double pulley", repo.getPreviousEntryNotes(listOf(FLY), "live", brno.id).getValue(FLY).text)
        assertTrue(repo.getLastSets(listOf(FLY), null).isEmpty())
        assertTrue(repo.getPreviousEntryNotes(listOf(FLY), "live", null).isEmpty())
        assertEquals(1, repo.getExerciseProgress(FLY, repo.locationScope(FLY, other.id)).size)

        repo.setTrackPerLocation(FLY, false)
        assertEquals(59.0, repo.getLastSets(listOf(FLY), brno.id).getValue(FLY).single().weightKg, 0.0)
        assertEquals(2, repo.getExerciseProgress(FLY, repo.locationScope(FLY, other.id)).size)

        repo.muteSplitPrompt(FLY)
        assertTrue(repo.getExercise(FLY)!!.splitPromptMuted)
    }

    @Test
    fun machineNotesPerExerciseAndPlace() = runBlocking {
        repo.saveExercise(Exercise(id = FLY, name = "Cable Fly", trackPerLocation = true))
        repo.saveExercise(Exercise(id = ROW, name = "Row"))
        val brno = repo.saveLocation(Location(name = "Brno gym"))
        val other = repo.saveLocation(Location(name = "Other gym"))
        repo.saveLocationNote(FLY, brno.id, "  Seat 3  ")
        repo.saveLocationNote(FLY, brno.id, "Double pulley, seat 4")
        repo.saveLocationNote(FLY, other.id, "Single pulley")
        repo.saveLocationNote(ROW, brno.id, "Plate loaded")

        assertEquals("Double pulley, seat 4", repo.observeLocationNote(FLY, brno.id).first())
        assertEquals("", repo.observeLocationNote(FLY, Location.HOME_ID).first())
        assertEquals(
            mapOf(FLY to "Double pulley, seat 4", ROW to "Plate loaded"),
            repo.getLocationNotes(listOf(FLY, ROW, ROW), brno.id),
        )
        assertEquals(mapOf(brno.id to "Double pulley, seat 4", other.id to "Single pulley"), repo.observeLocationNotesFor(FLY).first())
        assertTrue(repo.getLocationNotes(emptyList(), brno.id).isEmpty())

        val bundle = repo.createExportBundle()
        assertEquals(3, bundle.locationNotes.size)
        repo.saveLocationNote(ROW, brno.id, "   ")
        assertEquals(2, repo.getAllLocationNotes().size)

        repo.deleteAllWorkoutData()
        assertTrue(repo.getAllLocationNotes().isEmpty())
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), repo.getLocations().map { it.id })
        repo.importBundle(bundle)
        assertEquals(bundle.locationNotes.toSet(), repo.getAllLocationNotes().toSet())
        assertEquals("Brno gym", repo.getLocation(brno.id)!!.name)

        repo.saveLocationNote(FLY, brno.id, "Newer note")
        repo.restoreLocationNotes(bundle.locationNotes + ExerciseLocationNote(FLY, Location.HOME_ID, "At home"))
        assertEquals("Newer note", repo.getLocationNotes(listOf(FLY), brno.id).getValue(FLY))
        assertEquals("At home", repo.getLocationNotes(listOf(FLY), Location.HOME_ID).getValue(FLY))
    }

    // --- Export, import, restore, delete all ------------------------------------------------------

    @Test
    fun exportThenImportIntoAFreshStoreGivesTheSameBundle() = runBlocking {
        repo.ensureSeeded()
        repo.saveTemplate(WorkoutTemplate(name = "Push"))
        repo.saveWorkoutSession(completedWithChildren())
        repo.saveLocation(Location(id = "gym", name = "Gym"))
        repo.saveLocationNote("bench", "gym", "Seat 4")
        val original = repo.createExportBundle()

        val fresh = newStore().repo
        fresh.importBundle(JsonExporter.fromJson(JsonExporter.toJson(original)))
        assertEquals(original.copy(exportedAtUtc = ""), fresh.createExportBundle().copy(exportedAtUtc = ""))
    }

    @Test
    fun runsAndRoutesSurviveATransferIntoAFreshStore() = runBlocking {
        runs.saveRoute(sampleRoute())
        runs.saveRun(sampleRun())
        assertEquals(1, runs.countRuns())
        assertTrue(runs.getRuns().single().trace.isEmpty())
        assertTrue(runs.getRoutes().single().points.isEmpty())
        assertEquals(runs.getRuns(), runs.observeRuns().first())
        val json = DataTransferService(repo, runs).exportJson().text

        val fresh = newStore()
        val service = DataTransferService(fresh.repo, fresh.runs)
        val preview = service.previewImport(json, "workout-backup.json")
        assertTrue(preview.canCommit)
        assertEquals(1, preview.summary.insertedRuns)
        service.commitImport(preview)
        val restored = fresh.runs.getRun("run1")!!
        assertEquals(3, restored.trace.size)
        assertTrue(restored.trace[1].segmentStart)
        assertEquals(2, fresh.runs.getRoute("route1")!!.points.size)
        assertEquals(0, service.previewImport(json, "workout-backup.json").summary.insertedRuns)

        fresh.runs.updateRunNotes("run1", "Evening")
        assertEquals("Evening", fresh.runs.getRun("run1")!!.title)
        fresh.runs.deleteRun("run1")
        fresh.runs.deleteRoute("route1")
        assertEquals(0, fresh.runs.countRuns())
        assertEquals(0, fresh.runs.countRoutes())
    }

    @Test
    fun aReplaceRestorePutsBackTheWholeStore() = runBlocking {
        val settings = MemorySettingsArchive(SettingsSnapshot(themeMode = "Dark", units = "Imperial", defaultRestSeconds = 150))
        val service = DataTransferService(repo, runs, photos = NoPhotoArchive, settings = settings, appVersion = "9.9.9")
        val bench = repo.saveExercise(Exercise(name = "Bench"))
        repo.saveWorkoutSession(workout("kept", at = 1_000, location = null, kg = 50.0).copy(entries = emptyList()))
        runs.saveRoute(sampleRoute())
        runs.saveRun(sampleRun())
        val backup = service.exportJson().text
        assertEquals("9.9.9", JsonExporter.fromJson(backup).appVersion)

        repo.saveExercise(Exercise(name = "Extra"))
        repo.saveWorkoutSession(workout("extra", at = 2_000, location = null, kg = 50.0))
        runs.deleteAll()
        settings.current = SettingsSnapshot()

        val preview = service.previewImport(backup, "backup.json", ImportOptions(mode = RestoreMode.Replace))
        assertTrue(preview.canCommit)
        val result = service.commitImport(preview)
        assertEquals(RestoreMode.Replace, result.mode)
        assertEquals(listOf("kept"), repo.getSessions().map { it.id })
        assertEquals(listOf(bench.id), repo.getExercises(ExerciseFilter(includeArchived = true)).map { it.id })
        assertEquals(3, runs.getRun("run1")?.trace?.size)
        assertEquals("Dark", settings.current.themeMode)
    }

    @Test
    fun aMergeImportAddsWithoutDuplicating() = runBlocking {
        repo.ensureSeeded()
        val service = DataTransferService(repo, runs)
        val incoming = ExportBundle(
            exercises = listOf(Exercise(id = "ex1", name = "Hack Squat")),
            sessions = listOf(workout("incoming", at = 5_000, location = null, kg = 80.0)),
        )
        val json = JsonExporter.toJson(incoming)
        val first = service.commitImport(service.previewImport(json, "a.json"))
        assertEquals(1, first.insertedSessions)
        assertEquals(17, repo.countExercises())
        val second = service.commitImport(service.previewImport(json, "a.json"))
        assertEquals(0, second.insertedSessions)
        assertEquals(1, repo.countWorkouts())
    }

    @Test
    fun aFailedRestoreChangesNothing() = runBlocking {
        repo.saveWorkoutSession(workout("kept", at = 1_000, location = null, kg = 20.0))
        val brokenRuns = object : RunRepository by runs {
            override suspend fun saveRun(run: Run) = error("disk full")
        }
        val bundle = ExportBundle(
            exercises = listOf(Exercise(id = "ex1", name = "Squat")),
            sessions = listOf(workout("incoming", at = 2_000, location = null, kg = 20.0)),
            runs = listOf(sampleRun()),
        )
        val service = DataTransferService(repo, brokenRuns)
        val preview = service.previewImport(JsonExporter.toJson(bundle), "backup.json")
        try {
            service.commitImport(preview)
            fail("The restore should have failed")
        } catch (expected: IllegalStateException) {
            assertEquals("disk full", expected.message)
        }
        assertEquals(listOf("kept"), repo.getSessions().map { it.id })
        assertNull(repo.getExercise("ex1"))
    }

    @Test
    fun aLyftaImportIsIdempotent() = runBlocking {
        val csv = checkNotNull(javaClass.classLoader?.getResource("lyfta-sample.csv")) { "the Lyfta sample is missing" }.readText()
        repo.ensureSeeded()
        val service = DataTransferService(repo, runs)
        val options = ImportOptions(sourceTimeZoneId = "Europe/Prague")
        val committed = service.commitImport(service.previewImport(csv, "lyfta-sample.csv", options))
        assertEquals(2, committed.insertedSessions)
        assertEquals(17, repo.countExercises())
        val again = service.commitImport(service.previewImport(csv, "lyfta-sample.csv", options))
        assertEquals(2, again.skippedSessions)
        assertEquals(2, repo.getSessions().size)
    }

    @Test
    fun deleteAllDataReturnsToAFreshInstall() = runBlocking {
        repo.ensureSeeded()
        repo.saveExercise(Exercise(name = "Mine"))
        repo.saveTemplate(WorkoutTemplate(name = "Push"))
        repo.saveWorkoutSession(completedWithChildren())
        repo.saveLocation(Location(name = "Gym"))
        runs.saveRun(sampleRun())
        val settings = MemorySettingsArchive(SettingsSnapshot(themeMode = "Dark"))
        val eraser = UserDataEraser(repo, runs, NoPhotoArchive, settings, stopAutomaticBackup = {}, isRunRecording = { false })

        repo.createWorkoutSession()
        assertEquals("Finish or discard the live workout first.", eraser.blocker())
        repo.deleteWorkoutSession(repo.getActiveSession()!!.id)

        eraser.eraseAll()
        assertEquals(16, repo.countExercises())
        assertEquals(0, repo.countTemplates())
        assertEquals(0, repo.countWorkouts())
        assertEquals(0, runs.countRuns())
        assertEquals(listOf(Location.HOME_ID, Location.OUTDOORS_ID), repo.getLocations().map { it.id })
        assertEquals(SettingsSnapshot(), settings.current)
        assertNotNull(repo.getExercises().firstOrNull { it.name == "Back Squat" })
    }

    // --- Fixtures --------------------------------------------------------------------------------

    protected fun completedWithChildren() = WorkoutSession(
        name = "Push A",
        startedAtUtc = 1_700_000_000_000L,
        endedAtUtc = 1_700_003_600_000L,
        completedDateUtc = 1_700_003_600_000L,
        durationSeconds = 3600,
        status = WorkoutSessionStatus.Completed,
        entries = listOf(
            WorkoutEntry(
                exerciseId = "bench", exerciseSnapshotName = "Bench", exerciseSnapshotPrimaryBodyPart = "Chest",
                strengthSets = listOf(StrengthSet(reps = 5, weightKg = 100.0), StrengthSet(reps = 3, weightKg = 110.0)),
                cardioData = CardioEntryData(durationSeconds = 9),
            ),
            WorkoutEntry(
                exerciseSnapshotName = "", entryType = ExerciseCategory.Cardio, sortOrder = 4,
                strengthSets = listOf(StrengthSet(reps = 1)),
                cardioData = CardioEntryData(durationSeconds = 1200, distanceKm = 6.4),
            ),
        ),
    )

    protected fun workout(id: String, at: Long, location: String?, kg: Double, note: String = "") = WorkoutSession(
        id = id,
        name = id,
        startedAtUtc = at - 500,
        completedDateUtc = at,
        status = WorkoutSessionStatus.Completed,
        locationId = location,
        entries = listOf(
            WorkoutEntry(
                exerciseId = FLY,
                exerciseSnapshotName = "Cable Fly",
                notes = note,
                strengthSets = listOf(StrengthSet(reps = 10, weightKg = kg, performedAtUtc = at)),
            ),
        ),
    )

    protected fun sampleRoute() = Route(
        id = "route1", name = "River loop", createdAtUtc = 1_700_000_000_000L,
        points = listOf(RoutePoint(0, 50.08, 14.42), RoutePoint(1, 50.081, 14.421)),
    )

    protected fun sampleRun() = Run(
        id = "run1", startedAtUtc = 1_700_000_000_000L, durationSeconds = 600, movingSeconds = 590,
        distanceMeters = 1000.0, routeId = "route1",
        trace = listOf(
            TracePoint(t = 0, lat = 50.08, lon = 14.42),
            TracePoint(t = 300_000, lat = 50.10, lon = 14.42, segmentStart = true),
            TracePoint(t = 360_000, lat = 50.101, lon = 14.42),
        ),
    )

    protected companion object {
        const val FLY = "cable-fly"
        const val ROW = "row"
    }
}
