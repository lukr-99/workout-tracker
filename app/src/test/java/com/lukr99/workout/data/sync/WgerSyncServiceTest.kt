package com.lukr99.workout.data.sync

import com.lukr99.workout.data.ExternalExerciseMergeSummary
import com.lukr99.workout.domain.Exercise
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WgerSyncServiceTest {
    @Test
    fun mapsPagesAndReturnsStructuredMergeOutcome() = runTest {
        val merged = mutableListOf<Exercise>()
        val pages = mutableListOf<String>()
        val service = WgerSyncService(
            merger = ExternalExerciseMerger {
                merged += it
                ExternalExerciseMergeSummary(added = 1, updated = 1, skipped = 0)
            },
            pageSource = WgerPageSource { url ->
                pages += url
                if (pages.size == 1) {
                    WgerPage(
                        next = "https://wger.de/api/v2/exerciseinfo/?offset=2",
                        results = listOf(validRemote("uuid-1", "Bench press"), invalidRemote()),
                    )
                } else {
                    WgerPage(results = listOf(validRemote("uuid-2", "Squat")))
                }
            },
        )

        val result = service.sync(WgerSyncOptions(limit = 3, pageSize = 2))

        assertEquals(2, pages.size)
        assertEquals(3, result.fetched)
        assertEquals(2, result.mapped)
        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        assertEquals(1, result.skipped)
        assertEquals(listOf("wger:uuid-1", "wger:uuid-2"), merged.map(Exercise::externalSourceId))
        assertEquals("Chest", merged.first().primaryBodyPart)
        assertEquals(listOf("Triceps"), merged.first().secondaryBodyParts)
        assertEquals("Barbell", merged.first().equipment)
        assertEquals("Use control & breathe.", merged.first().notes)
    }

    @Test
    fun rejectsPagingOutsideConfiguredOrigin() = runTest {
        val service = WgerSyncService(
            merger = ExternalExerciseMerger { ExternalExerciseMergeSummary(added = it.size) },
            pageSource = WgerPageSource {
                WgerPage(next = "https://example.com/redirect", results = listOf(validRemote("one", "One")))
            },
        )

        val result = service.sync(WgerSyncOptions(limit = 10))

        assertEquals(1, result.pages)
        assertTrue(result.warnings.single().contains("outside"))
    }

    @Test
    fun publicPayloadParsingIsTolerantOfUnknownFields() {
        val page = PayloadJson.decodeFromString<WgerPage>(
            """
            {
              "count": 1,
              "next": null,
              "unknown_top_level": true,
              "results": [{
                "id": 42,
                "uuid": "payload-id",
                "category": {"id": 9, "name": "Strength"},
                "muscles": [{"id": 4, "name": "Chest", "name_en": "Chest"}],
                "muscles_secondary": [{"id": 5, "name": "Triceps"}],
                "equipment": [{"id": 1, "name": "Barbell"}],
                "translations": [{"language": 2, "name": "Bench press"}],
                "future_field": {"is_safe": true}
              }]
            }
            """.trimIndent(),
        )

        val mapped = page.results.single().toExercise(preferredLanguage = 2)!!
        assertEquals("Bench press", mapped.name)
        assertEquals("wger:payload-id", mapped.externalSourceId)
    }

    private fun validRemote(id: String, name: String) = WgerExerciseDto(
        uuid = id,
        category = WgerNamedDto(name = "Strength"),
        muscles = listOf(WgerNamedDto(name = "Chest")),
        secondaryMuscles = listOf(WgerNamedDto(name = "Triceps")),
        equipment = listOf(WgerNamedDto(name = "Barbell")),
        translations = listOf(
            WgerTranslationDto(
                language = 2,
                name = name,
                descriptionSource = "<p>Use control &amp; breathe.</p>",
            ),
        ),
    )

    private fun invalidRemote() = WgerExerciseDto(uuid = "invalid")

    private companion object {
        val PayloadJson = Json { ignoreUnknownKeys = true }
    }
}
