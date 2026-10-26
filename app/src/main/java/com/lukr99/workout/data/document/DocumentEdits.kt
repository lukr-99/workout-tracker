package com.lukr99.workout.data.document

import com.lukr99.workout.data.export.ExportBundle
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseLocationNote
import com.lukr99.workout.domain.Location
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus
import com.lukr99.workout.domain.WorkoutTemplate

/*
 * Small pure edits and reads on a WorkoutDocument, shared by the document repositories. Lists keep
 * their order: an upsert replaces a row where it is and appends a new one, like a database keeps a
 * row's place, so ties in a sort come out in the same order every time.
 */

internal fun WorkoutDocument.edit(transform: ExportBundle.() -> ExportBundle): WorkoutDocument =
    copy(data = data.transform())

internal fun <T> List<T>.upsert(item: T, sameAs: (T) -> Boolean): List<T> {
    val index = indexOfFirst(sameAs)
    return if (index < 0) this + item else toMutableList().also { it[index] = item }
}

internal fun <T> List<T>.upsertAll(items: List<T>, key: (T) -> Any): List<T> =
    items.fold(this) { list, item -> list.upsert(item) { key(it) == key(item) } }

internal fun ExportBundle.upsertExercise(exercise: Exercise): ExportBundle =
    copy(exercises = exercises.upsert(exercise) { it.id == exercise.id })

internal fun ExportBundle.upsertTemplate(template: WorkoutTemplate): ExportBundle =
    copy(templates = templates.upsert(template) { it.id == template.id })

internal fun ExportBundle.upsertSession(session: WorkoutSession): ExportBundle =
    copy(sessions = sessions.upsert(session) { it.id == session.id })

internal fun ExportBundle.upsertLocation(location: Location): ExportBundle =
    copy(locations = locations.upsert(location) { it.id == location.id })

internal fun ExportBundle.upsertLocationNote(note: ExerciseLocationNote): ExportBundle =
    copy(locationNotes = locationNotes.upsert(note) { it.exerciseId == note.exerciseId && it.locationId == note.locationId })

internal fun ExportBundle.mapExercise(id: String, change: (Exercise) -> Exercise): ExportBundle =
    copy(exercises = exercises.map { if (it.id == id) change(it) else it })

internal fun ExportBundle.mapSessions(change: (WorkoutSession) -> WorkoutSession): ExportBundle =
    copy(sessions = sessions.map(change))

/** Strength before cardio, then by name: the catalog order. */
internal fun WorkoutDocument.sortedExercises(): List<Exercise> =
    data.exercises.sortedWith(compareBy({ it.category.ordinal }, { it.name }))

internal fun WorkoutDocument.sortedTemplates(): List<WorkoutTemplate> = data.templates.sortedBy { it.name }

/** Gyms first, then Home and Outdoors, each by name. */
internal fun WorkoutDocument.sortedLocations(): List<Location> =
    data.locations.sortedWith(compareBy({ it.kind.ordinal }, { it.name }))

internal fun WorkoutDocument.sortedLocationNotes(): List<ExerciseLocationNote> =
    data.locationNotes.sortedWith(compareBy({ it.exerciseId }, { it.locationId }))

/** The newest live workout. */
internal fun WorkoutDocument.activeSession(): WorkoutSession? =
    data.sessions.filter { it.status == WorkoutSessionStatus.Active }.maxByOrNull { it.startedAtUtc }

/** Finished workouts, most recently finished first. */
internal fun WorkoutDocument.completedSessions(): List<WorkoutSession> =
    data.sessions.filter { it.status == WorkoutSessionStatus.Completed }
        .sortedWith(compareBy(nullsLast(reverseOrder())) { it.completedDateUtc })

/** Every workout but discarded ones, newest start first: the backup surface. */
internal fun WorkoutDocument.keptSessions(): List<WorkoutSession> =
    data.sessions.filter { it.status != WorkoutSessionStatus.Discarded }.sortedByDescending { it.startedAtUtc }

internal fun WorkoutDocument.allSessions(): List<WorkoutSession> = data.sessions.sortedByDescending { it.startedAtUtc }
