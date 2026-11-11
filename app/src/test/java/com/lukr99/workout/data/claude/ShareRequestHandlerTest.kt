package com.lukr99.workout.data.claude

import com.lukr99.workout.data.document.DocumentDatabase
import com.lukr99.workout.data.document.DocumentRunRepository
import com.lukr99.workout.data.document.DocumentWorkoutRepository
import com.lukr99.workout.data.document.FakeDocumentStore
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import com.lukr99.workout.settings.UnitSystem
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareRequestHandlerTest {
    private val server = FakeClaudeServer()
    private val store = MemoryClaudeLinkStore()
    private val database = DocumentDatabase(FakeDocumentStore().asStore())
    private val repository = DocumentWorkoutRepository(database)
    private val runs = DocumentRunRepository(database)
    private val handler = ShareRequestHandler(
        ClaudeApi(store, server.client),
        ClaudeShareBuilder(repository, runs, FixedSettingsStore(UnitSystem.Imperial), "2.11.0") { "Europe/Prague" },
        store,
    )

    init {
        server.answer("POST", "/v1/share-requests/$REQUEST/share", 200, """{"sharedAt":"2026-11-11T10:00:00Z","keepUntil":"2026-11-11T10:30:00Z","counts":{}}""")
        server.answer("POST", "/v1/share-requests/$REQUEST/decline", 204, "")
    }

    @Test
    fun askLeavesTheRequestToTheOwnerAndAllowShares() = runTest {
        assertEquals(ShareAnswer.NeedsOwner, handler.handle(REQUEST))
        assertTrue(server.sent.isEmpty())

        assertEquals(ShareAnswer.Shared, handler.allow(REQUEST))
        assertEquals("/v1/share-requests/$REQUEST/share", server.sent.single().path)
    }

    @Test
    fun automaticSharesTheTrainingWithoutPhotosTracesOrSettings() = runTest {
        store.setShareMode(ShareMode.Automatic)
        repository.ensureSeeded()
        val bench = repository.saveExercise(Exercise(name = "Test Bench", primaryBodyPart = "Chest"))
        val session = repository.createWorkoutSession(name = "Push")
        repository.saveWorkoutSession(
            session.copy(
                status = WorkoutSessionStatus.Completed,
                completedDateUtc = session.startedAtUtc,
                entries = listOf(
                    WorkoutEntry(
                        workoutSessionId = session.id,
                        exerciseId = bench.id,
                        exerciseSnapshotName = bench.name,
                        exerciseSnapshotPrimaryBodyPart = "Chest",
                        strengthSets = listOf(StrengthSet(setNumber = 1, reps = 5, weightKg = 82.5, rir = 2.0)),
                    ),
                ),
            ),
        )
        runs.saveRun(Run(id = "run-1", startedAtUtc = 1_762_800_000_000, durationSeconds = 600, distanceMeters = 2000.0, trace = straightTrace()))

        assertEquals(ShareAnswer.Shared, handler.handle(REQUEST))

        val body = ClaudeShareBuilder.json.parseToJsonElement(server.sent.single().body).jsonObject
        assertEquals(ClaudeShare.FORMAT, body.getValue("shareFormat").jsonPrimitive.int)
        assertEquals("imperial", body.getValue("units").jsonPrimitive.content)
        assertEquals("Europe/Prague", body.getValue("timeZone").jsonPrimitive.content)
        val bundle = body.getValue("bundle").jsonObject
        assertTrue(bundle.getValue("photos").jsonArray.isEmpty())
        assertEquals("null", bundle.getValue("settings").toString())
        assertTrue(bundle.getValue("runs").jsonArray.isEmpty())
        val pushSession = bundle.getValue("sessions").jsonArray.map { it.jsonObject }.single { it.getValue("name").jsonPrimitive.content == "Push" }
        assertEquals(1, pushSession.getValue("status").jsonPrimitive.int)
        val run = body.getValue("runs").jsonArray.single().jsonObject
        assertFalse(run.containsKey("trace"))
        assertEquals(2, run.getValue("splits").jsonArray.count { it.jsonObject.getValue("isFull").jsonPrimitive.content == "true" })
    }

    @Test
    fun neverDeclines() = runTest {
        store.setShareMode(ShareMode.Never)

        assertEquals(ShareAnswer.Declined, handler.handle(REQUEST))
        assertEquals("/v1/share-requests/$REQUEST/decline", server.sent.single().path)
    }

    @Test
    fun anAnswerAfterTheRequestLapsedIsExpired() = runTest {
        server.answer("POST", "/v1/share-requests/$REQUEST/share", 409, """{"error":{"code":"expired","message":"That request was answered or ran out."}}""")

        assertEquals(ShareAnswer.Expired, handler.allow(REQUEST))
    }

    @Test
    fun catchUpAnswersMissedRequestsAndReturnsTheOnesForTheOwner() = runTest {
        server.answer("GET", "/v1/share-requests", 200, """{"requests":[{"id":"$REQUEST","createdAt":"2026-11-11T10:00:00Z","expiresAt":"2026-11-11T10:10:00Z"}]}""")

        assertEquals(listOf(REQUEST), handler.catchUp().map(ShareRequest::id))

        store.setShareMode(ShareMode.Automatic)
        assertTrue(handler.catchUp().isEmpty())
        assertTrue(server.sent.any { it.path.endsWith("/share") })
    }

    @Test
    fun catchUpDoesNothingBeforeThePhoneIsConnected() = runTest {
        store.setLink(null)

        assertTrue(handler.catchUp().isEmpty())
        assertTrue(server.sent.isEmpty())
    }

    /** About 2.3 km due north at 5:30 per kilometre: two full splits and a short one. */
    private fun straightTrace(): List<TracePoint> = (0..23).map { i ->
        TracePoint(t = 1_762_800_000_000 + i * 33_000L, lat = 50.0 + i * 0.0009, lon = 14.4)
    }

    private companion object {
        const val REQUEST = "0123456789abcdef0123456789abcdef"
    }
}
