package com.lukr99.workout.domain

/**
 * Pure text for sharing a finished workout: the per-exercise lines on the share image and the plain
 * text summary for chats. Weights follow each exercise's unit override, then the owner's units.
 */
object WorkoutShareSummary {

    /** Exercises with something logged, in workout order. Lifts show their best working set. */
    fun exerciseLines(session: WorkoutSession, imperial: Boolean): List<ShareExerciseLine> =
        session.entries.sortedBy { it.sortOrder }.mapNotNull { entry ->
            val detail = if (entry.isStrength) {
                bestSet(countedSets(entry))?.let { formatSet(it, entryImperial(entry, imperial)) }
            } else {
                cardioDetail(entry.cardioData, imperial)
            } ?: return@mapNotNull null
            ShareExerciseLine(
                name = entry.exerciseSnapshotName.ifBlank { "Exercise" },
                detail = detail,
                hasPr = entry.strengthSets.any { it.isPr },
            )
        }

    /**
     * Plain text for a chat message: the name, [dateLabel], then each exercise with every set.
     * Warm-ups are marked, or left out when [includeWarmups] is false.
     */
    fun text(
        session: WorkoutSession,
        imperial: Boolean,
        dateLabel: String,
        includeWarmups: Boolean = true,
    ): String = buildString {
        appendLine(session.name.ifBlank { "Workout" })
        appendLine(dateLabel)
        session.entries.sortedBy { it.sortOrder }.forEach { entry ->
            val lines = if (entry.isStrength) {
                val units = entryImperial(entry, imperial)
                countedSets(entry)
                    .filter { includeWarmups || !it.isWarmupSet }
                    .map { set ->
                        buildString {
                            append(formatSet(set, units))
                            if (set.isWarmupSet) append(" (warm-up)")
                            if (set.isPr) append(" PR")
                        }
                    }
            } else {
                listOfNotNull(cardioDetail(entry.cardioData, imperial))
            }
            if (lines.isEmpty()) return@forEach
            appendLine()
            appendLine(entry.exerciseSnapshotName.ifBlank { "Exercise" })
            lines.forEach { appendLine("- $it") }
        }
        appendLine()
        append("Logged with Ember")
    }

    /** The heaviest working set, more reps breaking a tie. Warm-ups count only when nothing else does. */
    fun bestSet(sets: List<StrengthSet>): StrengthSet? {
        val withReps = sets.filter { it.reps > 0 }
        val working = withReps.filterNot { it.isWarmupSet }.ifEmpty { withReps }
        return working.maxWithOrNull(compareBy<StrengthSet> { it.weightKg }.thenBy { it.reps })
    }

    /** `100 kg x 5`, or `12 reps` for a set with no added weight. */
    fun formatSet(set: StrengthSet, imperial: Boolean): String {
        if (set.weightKg <= 0.0) return if (set.reps == 1) "1 rep" else "${set.reps} reps"
        val weight = if (imperial) Units.kgToLb(set.weightKg) else set.weightKg
        return "${Units.formatWeight(weight)} ${if (imperial) "lb" else "kg"} x ${set.reps}"
    }

    // Imported and older entries did not record performedAtUtc, so they fall back to every set.
    private fun countedSets(entry: WorkoutEntry): List<StrengthSet> {
        val performed = entry.strengthSets.filter { it.performedAtUtc != null }
        return performed.ifEmpty { entry.strengthSets }.filter { it.reps > 0 }
    }

    private fun entryImperial(entry: WorkoutEntry, imperial: Boolean): Boolean = when (entry.weightUnitOverride) {
        WeightDisplayUnit.Pounds -> true
        WeightDisplayUnit.Kilograms -> false
        null -> imperial
    }

    private fun cardioDetail(cardio: CardioEntryData?, imperial: Boolean): String? {
        if (cardio == null) return null
        val parts = buildList {
            if (cardio.durationSeconds > 0) add(Units.formatDuration(cardio.durationSeconds.toLong()))
            cardio.distanceKm?.takeIf { it > 0 }?.let { km ->
                add(if (imperial) "%.2f mi".format(Units.kmToMiles(km)) else "%.2f km".format(km))
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private val StrengthSet.isWarmupSet: Boolean get() = isWarmup || SetTag.Warmup in effectiveTags
}
