package com.lukr99.workout.data

import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutTemplate
import com.lukr99.workout.domain.WorkoutTemplateExercise
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/*
 * Maps the Room entities and relations to the domain model and back. Only the repository uses
 * these. List columns are stored as JSON text.
 */

internal fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    category = category,
    primaryBodyPart = primaryBodyPart,
    secondaryBodyParts = decodeList(secondaryBodyPartsJson),
    equipment = equipment,
    notes = notes,
    source = source,
    externalSourceId = externalSourceId,
    isArchived = isArchived,
    defaultRestSeconds = defaultRestSeconds,
    imageUrl = imageUrl,
    imageAttribution = imageAttribution,
    localImagePath = localImagePath,
    instructions = instructions,
    videoUrl = videoUrl,
)

internal fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    category = category,
    primaryBodyPart = primaryBodyPart,
    secondaryBodyPartsJson = encodeList(secondaryBodyParts),
    equipment = equipment,
    notes = notes,
    source = source,
    externalSourceId = externalSourceId,
    isArchived = isArchived,
    defaultRestSeconds = defaultRestSeconds,
    imageUrl = imageUrl,
    imageAttribution = imageAttribution,
    localImagePath = localImagePath,
    instructions = instructions,
    videoUrl = videoUrl,
)

internal fun TemplateWithExercises.toDomain() = WorkoutTemplate(
    id = template.id,
    name = template.name,
    notes = template.notes,
    exercises = exercises.sortedBy { it.sortOrder }.map {
        WorkoutTemplateExercise(
            id = it.id,
            exerciseId = it.exerciseId,
            exerciseName = it.exerciseName,
            category = it.category,
            bodyPart = it.bodyPart,
            sortOrder = it.sortOrder,
            notes = it.notes,
        )
    },
)

internal fun WorkoutSession.toEntity() = SessionEntity(
    id = id,
    templateId = templateId,
    name = name,
    status = status,
    startedAtUtc = startedAtUtc,
    endedAtUtc = endedAtUtc,
    completedDateUtc = completedDateUtc,
    durationSeconds = durationSeconds,
    notes = notes,
    perceivedEffort = perceivedEffort,
    bodyweightKg = bodyweightKg,
    source = source,
    externalKey = externalKey,
)

internal fun WorkoutEntry.toEntity() = EntryEntity(
    id = id,
    workoutSessionId = workoutSessionId,
    exerciseId = exerciseId,
    exerciseSnapshotName = exerciseSnapshotName.ifBlank { "Exercise" },
    exerciseSnapshotCategory = exerciseSnapshotCategory,
    exerciseSnapshotPrimaryBodyPart = exerciseSnapshotPrimaryBodyPart,
    sortOrder = sortOrder,
    entryType = entryType,
    notes = notes,
    supersetGroup = supersetGroup,
    weightUnitOverride = weightUnitOverride,
    startedAtUtc = startedAtUtc,
    completedAtUtc = completedAtUtc,
)

internal fun StrengthSet.toEntity() = StrengthSetEntity(
    id = id,
    workoutEntryId = workoutEntryId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    rir = rir,
    rpe = rpe,
    performedAtUtc = performedAtUtc,
    notes = notes,
    isWarmup = isWarmup,
    isPr = isPr,
    durationSeconds = durationSeconds,
    setType = setType,
    tagsJson = encodeIntList(tags.map { it.ordinal }.sorted()),
)

internal fun CardioEntryData.toEntity() = CardioDataEntity(
    workoutEntryId = workoutEntryId,
    durationSeconds = durationSeconds,
    distanceKm = distanceKm,
    calories = calories,
    notes = notes,
)

internal fun SessionWithEntries.toDomain() = WorkoutSession(
    id = session.id,
    templateId = session.templateId,
    name = session.name,
    startedAtUtc = session.startedAtUtc,
    endedAtUtc = session.endedAtUtc,
    completedDateUtc = session.completedDateUtc,
    durationSeconds = session.durationSeconds,
    notes = session.notes,
    status = session.status,
    perceivedEffort = session.perceivedEffort,
    bodyweightKg = session.bodyweightKg,
    source = session.source,
    externalKey = session.externalKey,
    entries = entries.sortedBy { it.entry.sortOrder }.map { it.toDomain() },
)

internal fun EntryWithSets.toDomain() = WorkoutEntry(
    id = entry.id,
    workoutSessionId = entry.workoutSessionId,
    exerciseId = entry.exerciseId,
    exerciseSnapshotName = entry.exerciseSnapshotName,
    exerciseSnapshotCategory = entry.exerciseSnapshotCategory,
    exerciseSnapshotPrimaryBodyPart = entry.exerciseSnapshotPrimaryBodyPart,
    sortOrder = entry.sortOrder,
    entryType = entry.entryType,
    notes = entry.notes,
    supersetGroup = entry.supersetGroup,
    weightUnitOverride = entry.weightUnitOverride,
    startedAtUtc = entry.startedAtUtc,
    completedAtUtc = entry.completedAtUtc,
    strengthSets = strengthSets.sortedBy { it.setNumber }.map { it.toDomain() },
    cardioData = cardio?.toDomain(),
)

internal fun StrengthSetEntity.toDomain() = StrengthSet(
    id = id,
    workoutEntryId = workoutEntryId,
    setNumber = setNumber,
    reps = reps,
    weightKg = weightKg,
    rir = rir,
    rpe = rpe,
    performedAtUtc = performedAtUtc,
    notes = notes,
    isWarmup = isWarmup,
    isPr = isPr,
    durationSeconds = durationSeconds,
    setType = setType,
    tags = decodeIntList(tagsJson).mapNotNull { SetTag.entries.getOrNull(it) }.toSet(),
)

internal fun CardioDataEntity.toDomain() = CardioEntryData(
    workoutEntryId = workoutEntryId,
    durationSeconds = durationSeconds,
    distanceKm = distanceKm,
    calories = calories,
    notes = notes,
)

private val listJson = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())
private val intListSerializer = ListSerializer(Int.serializer())

private fun encodeList(list: List<String>): String = listJson.encodeToString(stringListSerializer, list)

private fun decodeList(value: String?): List<String> =
    if (value.isNullOrBlank()) emptyList()
    else runCatching { listJson.decodeFromString(stringListSerializer, value) }.getOrDefault(emptyList())

private fun encodeIntList(list: List<Int>): String = listJson.encodeToString(intListSerializer, list)

private fun decodeIntList(value: String?): List<Int> =
    if (value.isNullOrBlank()) emptyList()
    else runCatching { listJson.decodeFromString(intListSerializer, value) }.getOrDefault(emptyList())
