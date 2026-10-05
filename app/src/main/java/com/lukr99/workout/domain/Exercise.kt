package com.lukr99.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String = newId(),
    val name: String = "",
    val category: ExerciseCategory = ExerciseCategory.Strength,
    val primaryBodyPart: String = "",
    val secondaryBodyParts: List<String> = emptyList(),
    val equipment: String = "",
    val notes: String = "",
    val source: ExerciseSource = ExerciseSource.Custom,
    val externalSourceId: String? = null,
    val isArchived: Boolean = false,
    // Rework-additive (03-data-model.md).
    val defaultRestSeconds: Int? = null,
    // Optional remote/local artwork. Null keeps seeded and custom exercises offline-friendly.
    val imageUrl: String? = null,
    val imageAttribution: String? = null,
    // Exported as a reference only; the app-private image file remains device-local.
    val localImagePath: String? = null,
    // Export v1.7: how to perform it (one step per line) and an optional guide or video link.
    // [notes] stays the owner's personal note (seat height, grip) shown while logging.
    val instructions: String = "",
    val videoUrl: String? = null,
) {
    /** Non-blank instruction lines, in order. Derived, not persisted. */
    val instructionSteps: List<String>
        get() = instructions.lines().map(String::trim).filter(String::isNotBlank)

    /** Primary + distinct secondaries, joined for display. Derived — not persisted. */
    val bodyPartsSummary: String
        get() = (listOf(primaryBodyPart) + secondaryBodyParts)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .joinToString(" / ")
}
