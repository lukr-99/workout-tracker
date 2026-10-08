package com.lukr99.workout.domain

import kotlin.math.roundToInt

/** How a template's plan reads before it starts: step labels and roughly how long it takes. */
object TemplatePlanShape {

    /** Seconds one strength set takes to do, not counting the rest after it. */
    const val SET_SECONDS = 40

    /** Sets assumed for an exercise without a planned number, as the editor adds them. */
    const val UNPLANNED_SETS = 3

    /** A cardio exercise without a plan. */
    const val CARDIO_SECONDS = 10 * 60

    /** Walking to the next machine, loading the bar. */
    const val CHANGEOVER_SECONDS = 2 * 60

    /**
     * One label per exercise in order: "1", "2", and for a superset one number shared with letters,
     * "4a", "4b", since its exercises are done as one round.
     */
    fun labels(exercises: List<WorkoutTemplateExercise>): List<String> {
        var step = 0
        return exercises.mapIndexed { i, ex ->
            val group = ex.supersetGroup
            val samePrevious = group != null && exercises.getOrNull(i - 1)?.supersetGroup == group
            val sameNext = group != null && exercises.getOrNull(i + 1)?.supersetGroup == group
            if (!samePrevious) step++
            if (!samePrevious && !sameNext) {
                "$step"
            } else {
                val position = (i downTo 0).takeWhile { exercises[it].supersetGroup == group }.count() - 1
                "$step${'a' + position}"
            }
        }
    }

    /**
     * Roughly how long the plan takes, rounded to 5 minutes. Each set takes [SET_SECONDS] plus its
     * rest ([defaultRestSeconds] when the exercise has none); a superset rests once per round, after
     * its longest rest. Every step adds [CHANGEOVER_SECONDS].
     */
    fun estimatedMinutes(exercises: List<WorkoutTemplateExercise>, defaultRestSeconds: Int): Int {
        if (exercises.isEmpty()) return 0
        val steps = mutableListOf<MutableList<WorkoutTemplateExercise>>()
        exercises.forEachIndexed { i, ex ->
            val joined = ex.supersetGroup != null && exercises.getOrNull(i - 1)?.supersetGroup == ex.supersetGroup
            if (joined) steps.last() += ex else steps += mutableListOf(ex)
        }
        val seconds = steps.sumOf { step ->
            val strength = step.filter { it.category == ExerciseCategory.Strength }
            val cardio = step.count { it.category != ExerciseCategory.Strength } * CARDIO_SECONDS
            val rounds = strength.maxOfOrNull { it.targetSets ?: UNPLANNED_SETS } ?: 0
            val rest = strength.maxOfOrNull { it.restSeconds ?: defaultRestSeconds } ?: 0
            val work = strength.sumOf { (it.targetSets ?: UNPLANNED_SETS) * SET_SECONDS }
            // No rest after the last round: the changeover covers it.
            work + (rounds - 1).coerceAtLeast(0) * rest + cardio + CHANGEOVER_SECONDS
        }
        return ((seconds / 60.0 / 5).roundToInt() * 5).coerceAtLeast(5)
    }
}
