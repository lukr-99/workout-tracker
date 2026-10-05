package com.lukr99.workout.domain

/**
 * The exercise as it is stored: an id, a name and a primary body part are always set, secondary
 * body parts are trimmed and unique, and blank optional text becomes null.
 */
fun Exercise.normalized(): Exercise = copy(
    id = id.ifBlank { newId() },
    name = name.ifBlank { "Custom Exercise" }.trim(),
    primaryBodyPart = primaryBodyPart.ifBlank {
        if (category == ExerciseCategory.Cardio) "Cardio" else "Full Body"
    }.trim(),
    secondaryBodyParts = secondaryBodyParts
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .distinctBy { it.lowercase() },
    imageUrl = imageUrl?.trim()?.ifBlank { null },
    imageAttribution = imageAttribution?.trim()?.ifBlank { null },
    localImagePath = localImagePath?.trim()?.ifBlank { null },
    instructions = instructions.trim(),
    videoUrl = GuideLink.normalize(videoUrl),
)
