package com.lukr99.workout.data

import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseSource
import com.lukr99.workout.domain.normalized

/**
 * Plans an additive merge of an external catalog into the owner's catalog, without writing.
 *
 * Only rows already tagged [ExerciseSource.Synced] can change. Existing values win: the merge may
 * fill blank fields and add secondary body parts, but it never renames, unarchives, changes
 * defaults or overwrites notes. A name that collides with a custom or seed row is skipped, and so
 * is an incoming row without an external id or with one seen earlier in the same batch.
 */
object ExternalExerciseMerge {

    fun plan(existing: List<Exercise>, incoming: List<Exercise>): ExternalExerciseMergePlan {
        val byExternalId = existing
            .filter { !it.externalSourceId.isNullOrBlank() }
            .associateByTo(linkedMapOf()) { it.externalSourceId!!.trim().lowercase() }
        val byName = existing.associateByTo(linkedMapOf()) { it.name.trim().lowercase() }
        val seenExternalIds = mutableSetOf<String>()
        val upserts = mutableListOf<Exercise>()
        var summary = ExternalExerciseMergeSummary()

        for (raw in incoming) {
            val externalId = raw.externalSourceId?.trim()?.takeIf(String::isNotBlank)
            if (externalId == null || !seenExternalIds.add(externalId.lowercase())) {
                summary += ExternalExerciseMergeSummary(skipped = 1)
                continue
            }

            val candidate = raw.normalized().copy(
                source = ExerciseSource.Synced,
                externalSourceId = externalId,
            )
            val externalKey = externalId.lowercase()
            val externalMatch = byExternalId[externalKey]
            val nameMatch = byName[candidate.name.lowercase()]

            val written = when {
                externalMatch != null && externalMatch.source != ExerciseSource.Synced -> null
                externalMatch != null -> externalMatch.withExternalFields(candidate)
                    .takeIf { it != externalMatch }
                nameMatch != null -> null
                else -> candidate
            }
            if (written == null) {
                summary += ExternalExerciseMergeSummary(skipped = 1)
                continue
            }
            upserts += written
            byExternalId[externalKey] = written
            byName[written.name.lowercase()] = written
            summary += if (externalMatch != null) {
                ExternalExerciseMergeSummary(updated = 1)
            } else {
                ExternalExerciseMergeSummary(added = 1)
            }
        }
        return ExternalExerciseMergePlan(upserts, summary)
    }

    private fun Exercise.withExternalFields(incoming: Exercise): Exercise = copy(
        primaryBodyPart = primaryBodyPart.ifBlank { incoming.primaryBodyPart },
        secondaryBodyParts = (secondaryBodyParts + incoming.secondaryBodyParts)
            .filter(String::isNotBlank)
            .map(String::trim)
            .distinctBy(String::lowercase),
        equipment = equipment.ifBlank { incoming.equipment },
        notes = notes.ifBlank { incoming.notes },
        // Rows synced before v8 kept the description in notes; do not show it twice.
        instructions = instructions.ifBlank {
            incoming.instructions.takeUnless { it.trim() == notes.trim() }.orEmpty()
        },
        videoUrl = videoUrl ?: incoming.videoUrl,
        imageUrl = imageUrl ?: incoming.imageUrl,
        imageAttribution = imageAttribution ?: incoming.imageAttribution,
    )
}
