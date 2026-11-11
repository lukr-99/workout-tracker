package com.lukr99.workout.data.claude

import com.lukr99.workout.data.document.DocumentDatabase
import com.lukr99.workout.data.document.DocumentWorkoutRepository
import com.lukr99.workout.data.document.FakeDocumentStore
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProposalInboxTest {
    private val server = FakeClaudeServer()
    private val repository = DocumentWorkoutRepository(DocumentDatabase(FakeDocumentStore().asStore()))
    private val inbox = ProposalInbox(ClaudeApi(MemoryClaudeLinkStore(), server.client), repository)

    @Test
    fun acceptingATemplateCreatesItsNewExerciseWithThePlan() = runTest {
        repository.ensureSeeded()
        val bench = repository.getExercises().first()
        val proposal = pending(
            "p1",
            "template",
            """{"templateId":"t1","replacesTemplateId":null,"name":"Push B","notes":"Pairs with Push A.","exercises":[
              {"exerciseId":"${bench.id}","name":"${bench.name}","category":"strength","bodyPart":"Chest","notes":"RIR 2","targetSets":4,"repsMin":6,"repsMax":8,"restSeconds":180,"supersetGroup":null,"newExercise":null},
              {"exerciseId":"$NEW","name":"Incline Dumbbell Press","category":"strength","bodyPart":"Chest","notes":"","targetSets":3,"repsMin":10,"repsMax":12,"restSeconds":90,"supersetGroup":1,
               "newExercise":{"exerciseId":"$NEW","name":"Incline Dumbbell Press","category":"strength","primaryBodyPart":"Chest","secondaryBodyParts":["Shoulders"],"equipment":"Dumbbells","notes":"","instructions":"Bench at 30 degrees\nPress","defaultRestSeconds":90}}]}""",
        )
        server.answer("POST", "/v1/proposals/p1/decision", 200)

        inbox.accept(proposal)

        val created = repository.getExercise(NEW)!!
        assertEquals(ExerciseSource.Custom, created.source)
        assertEquals(listOf("Shoulders"), created.secondaryBodyParts)
        assertEquals(listOf("Bench at 30 degrees", "Press"), created.instructionSteps)
        assertEquals(90, created.defaultRestSeconds)
        val template = repository.getTemplate("t1")!!
        assertEquals("Push B", template.name)
        assertEquals(
            listOf(Triple(bench.name, 4, 6..8), Triple("Incline Dumbbell Press", 3, 10..12)),
            template.exercises.map { Triple(it.exerciseName, it.targetSets, it.repsMin!!..it.repsMax!!) },
        )
        assertEquals(listOf(null, 1), template.exercises.map(WorkoutTemplateExercise::supersetGroup))
        assertEquals("""{"decision":"accepted"}""", server.sent.last().body)
    }

    @Test
    fun aNewVersionReplacesTheTemplateAndAcceptingTwiceWritesTheSame() = runTest {
        repository.ensureSeeded()
        val bench = repository.getExercises().first()
        repository.saveTemplate(WorkoutTemplate(id = "t2", name = "Push A", exercises = listOf(WorkoutTemplateExercise(exerciseId = bench.id, exerciseName = bench.name, targetSets = 3))))
        val proposal = pending(
            "p2",
            "template",
            """{"templateId":"t2","replacesTemplateId":"t2","name":"Push A","exercises":[{"exerciseId":"${bench.id}","name":"${bench.name}","category":"strength","bodyPart":"Chest","targetSets":5,"repsMin":5,"repsMax":5}]}""",
        )
        server.answer("POST", "/v1/proposals/p2/decision", 200)

        inbox.accept(proposal)
        inbox.accept(proposal)

        val templates = repository.getTemplates().filter { it.id == "t2" }
        assertEquals(1, templates.size)
        assertEquals(5, templates.single().exercises.single().targetSets)
    }

    @Test
    fun rejectingChangesNothingHere() = runTest {
        val proposal = pending("p3", "exercise", """{"exerciseId":"$NEW","name":"Nordic Curl","category":"strength","primaryBodyPart":"Hamstrings"}""")
        server.answer("POST", "/v1/proposals/p3/decision", 200)

        inbox.reject(proposal)

        assertNull(repository.getExercise(NEW))
        assertEquals("""{"decision":"rejected"}""", server.sent.last().body)
    }

    /** A proposal as the server lists it, read through the real client. */
    private suspend fun pending(id: String, kind: String, payload: String): Proposal {
        server.answer(
            "GET",
            "/v1/proposals?status=pending",
            200,
            """{"proposals":[{"id":"$id","kind":"$kind","status":"pending","title":"x","reason":"Because.","payload":$payload,"createdAt":"2026-11-11T10:00:00Z","decidedAt":null}]}""",
        )
        return inbox.pending().single()
    }

    private companion object {
        const val NEW = "e0000000000000000000000000000001"
    }
}
