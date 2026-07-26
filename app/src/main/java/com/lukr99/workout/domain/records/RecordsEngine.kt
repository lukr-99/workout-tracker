package com.lukr99.workout.domain.records

import com.lukr99.workout.domain.Estimates
import com.lukr99.workout.domain.SetType
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionStatus

/** Deterministic personal-record computation over immutable workout history. */
object RecordsEngine {
    fun forExercise(
        sessions: Iterable<WorkoutSession>,
        exerciseId: String,
        repRange: IntRange = 1..12,
    ): ExerciseRecords {
        require(exerciseId.isNotBlank()) { "exerciseId must not be blank." }
        require(!repRange.isEmpty()) { "repRange must not be empty." }
        val candidates = sessions.asSequence()
            .filter { it.status == WorkoutSessionStatus.Completed }
            .flatMap { session ->
                session.entries.asSequence()
                    .filter { it.exerciseId == exerciseId }
                    .flatMap { entry ->
                        entry.strengthSets.asSequence()
                            .filter { it.isWorkingSet() }
                            .map { set -> SetCandidate(session, entry, set) }
                    }
            }
            .toList()
        val positiveLoad = candidates.filter { it.set.weightKg > 0.0 && it.set.reps > 0 }
        val sessionVolumes = candidates.groupBy { it.session.id }.values.map { group ->
            val first = group.first()
            SessionVolumeRecord(
                volumeKg = group.sumOf { it.set.weightKg * it.set.reps },
                setCount = group.size,
                source = first.source(setId = null),
            )
        }

        return ExerciseRecords(
            exerciseId = exerciseId,
            exerciseName = candidates.firstOrNull()?.entry?.exerciseSnapshotName.orEmpty(),
            heaviestSet = positiveLoad.bestSetBy { it.set.weightKg },
            bestEstimated1Rm = positiveLoad.bestSetBy { Estimates.epley(it.set.weightKg, it.set.reps) },
            bestSetVolume = positiveLoad.bestSetBy { it.set.weightKg * it.set.reps },
            bestSessionVolume = sessionVolumes.sortedWith(
                compareByDescending<SessionVolumeRecord> { it.volumeKg }
                    .thenBy { it.source.dateUtc }
                    .thenBy { it.source.sessionId },
            ).firstOrNull(),
            repMaxes = repRange.mapNotNull { reps ->
                positiveLoad.filter { it.set.reps == reps }
                    .bestCandidateBy { it.set.weightKg }
                    ?.let {
                        RepMaxRecord(
                            reps = reps,
                            weightKg = it.set.weightKg,
                            estimated1RmKg = Estimates.epley(it.set.weightKg, reps),
                            source = it.source(),
                        )
                    }
            },
        )
    }

    /**
     * Compare a just-entered working set with completed history. Strict improvement is required;
     * ties are not announced again.
     */
    fun evaluateSet(
        sessions: Iterable<WorkoutSession>,
        exerciseId: String,
        candidate: StrengthSet,
        repRange: IntRange = 1..12,
    ): RecordAchievements {
        if (!candidate.isWorkingSet() || candidate.weightKg <= 0 || candidate.reps <= 0) {
            return RecordAchievements()
        }
        val previous = forExercise(sessions, exerciseId, repRange)
        val kinds = linkedSetOf<RecordKind>()
        if (previous.heaviestSet == null || candidate.weightKg > previous.heaviestSet.value) {
            kinds += RecordKind.HeaviestSet
        }
        val e1rm = Estimates.epley(candidate.weightKg, candidate.reps)
        if (previous.bestEstimated1Rm == null || e1rm > previous.bestEstimated1Rm.value) {
            kinds += RecordKind.Estimated1Rm
        }
        val volume = candidate.weightKg * candidate.reps
        if (previous.bestSetVolume == null || volume > previous.bestSetVolume.value) {
            kinds += RecordKind.SetVolume
        }
        val repMax = previous.repMaxes.firstOrNull { it.reps == candidate.reps }
        val repMaxReps = if (
            candidate.reps in repRange && (repMax == null || candidate.weightKg > repMax.weightKg)
        ) {
            kinds += RecordKind.RepMax
            setOf(candidate.reps)
        } else {
            emptySet()
        }
        return RecordAchievements(kinds, repMaxReps)
    }

    /** Evaluate all sets plus the aggregate volume of a candidate session. */
    fun evaluateSession(
        sessions: Iterable<WorkoutSession>,
        candidateSession: WorkoutSession,
        exerciseId: String,
        repRange: IntRange = 1..12,
    ): RecordAchievements {
        val sets = candidateSession.entries
            .filter { it.exerciseId == exerciseId }
            .flatMap { it.strengthSets }
            .filter { it.isWorkingSet() }
        val combined = sets.fold(RecordAchievements()) { result, set ->
            result + evaluateSet(sessions, exerciseId, set, repRange)
        }
        if (sets.isEmpty()) return combined
        val previous = forExercise(sessions, exerciseId, repRange).bestSessionVolume
        val candidateVolume = sets.sumOf { it.weightKg * it.reps }
        return if (previous == null || candidateVolume > previous.volumeKg) {
            combined + RecordAchievements(setOf(RecordKind.SessionVolume))
        } else {
            combined
        }
    }

    private fun List<SetCandidate>.bestSetBy(value: (SetCandidate) -> Double): SetRecord? =
        bestCandidateBy(value)?.let { candidate ->
            SetRecord(
                value = value(candidate),
                weightKg = candidate.set.weightKg,
                reps = candidate.set.reps,
                setVolumeKg = candidate.set.weightKg * candidate.set.reps,
                estimated1RmKg = Estimates.epley(candidate.set.weightKg, candidate.set.reps),
                source = candidate.source(),
            )
        }

    private fun List<SetCandidate>.bestCandidateBy(value: (SetCandidate) -> Double): SetCandidate? =
        sortedWith(
            compareByDescending<SetCandidate>(value)
                .thenBy { it.dateUtc }
                .thenBy { it.session.id }
                .thenBy { it.entry.id }
                .thenBy { it.set.setNumber }
                .thenBy { it.set.id },
        ).firstOrNull()

    private fun StrengthSet.isWorkingSet(): Boolean =
        !isWarmup && setType != SetType.Warmup && reps >= 0 && weightKg >= 0

    private data class SetCandidate(
        val session: WorkoutSession,
        val entry: WorkoutEntry,
        val set: StrengthSet,
    ) {
        val dateUtc: Long = session.completedDateUtc ?: session.startedAtUtc

        fun source(setId: String? = set.id) = RecordSource(
            sessionId = session.id,
            sessionName = session.name,
            dateUtc = dateUtc,
            entryId = entry.id,
            setId = setId,
            setNumber = set.takeIf { setId != null }?.setNumber,
        )
    }
}

data class ExerciseRecords(
    val exerciseId: String,
    val exerciseName: String,
    val heaviestSet: SetRecord?,
    val bestEstimated1Rm: SetRecord?,
    val bestSetVolume: SetRecord?,
    val bestSessionVolume: SessionVolumeRecord?,
    val repMaxes: List<RepMaxRecord>,
)

data class SetRecord(
    /** The metric value represented by this record (kg, e1RM kg, or set-volume kg). */
    val value: Double,
    val weightKg: Double,
    val reps: Int,
    val setVolumeKg: Double,
    val estimated1RmKg: Double,
    val source: RecordSource,
)

data class SessionVolumeRecord(
    val volumeKg: Double,
    val setCount: Int,
    val source: RecordSource,
)

data class RepMaxRecord(
    val reps: Int,
    val weightKg: Double,
    val estimated1RmKg: Double,
    val source: RecordSource,
)

data class RecordSource(
    val sessionId: String,
    val sessionName: String,
    val dateUtc: Long,
    val entryId: String,
    val setId: String?,
    val setNumber: Int?,
)

enum class RecordKind { HeaviestSet, Estimated1Rm, SetVolume, SessionVolume, RepMax }

data class RecordAchievements(
    val kinds: Set<RecordKind> = emptySet(),
    val repMaxReps: Set<Int> = emptySet(),
) {
    val isPersonalRecord: Boolean get() = kinds.isNotEmpty()

    operator fun plus(other: RecordAchievements): RecordAchievements =
        RecordAchievements(kinds + other.kinds, repMaxReps + other.repMaxReps)
}
