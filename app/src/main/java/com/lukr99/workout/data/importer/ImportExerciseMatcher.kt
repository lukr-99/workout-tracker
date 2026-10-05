package com.lukr99.workout.data.importer

import com.lukr99.workout.data.transfer.ExerciseMatchMode
import com.lukr99.workout.data.transfer.ImportOptions
import com.lukr99.workout.domain.Exercise
import com.lukr99.workout.domain.ExerciseCategory
import kotlin.math.max

/**
 * Finds the catalog exercise an imported name means. How hard it tries depends on
 * [ImportOptions.exerciseMatchMode]: exact name, normalized name, aliases, then a fuzzy match.
 * Null means the import should create a new exercise.
 */
internal class ImportExerciseMatcher(
    private val catalog: List<Exercise>,
    private val options: ImportOptions,
) {
    private val exact = catalog.associateBy { it.name.trim().lowercase() }
    private val normalized = catalog.associateBy { normalizeExerciseName(it.name) }
    private val aliases = (BUILT_IN_EXERCISE_ALIASES + options.exerciseAliases.mapKeys { normalizeExerciseName(it.key) })
        .mapValues { normalizeExerciseName(it.value) }

    fun resolve(name: String, category: ExerciseCategory): Exercise? {
        if (options.exerciseMatchMode == ExerciseMatchMode.AlwaysCreate) return null
        exact[name.trim().lowercase()]?.let { return it }
        if (options.exerciseMatchMode == ExerciseMatchMode.Exact) return null
        val key = normalizeExerciseName(name)
        normalized[key]?.let { return it }
        if (options.exerciseMatchMode in setOf(ExerciseMatchMode.Aliases, ExerciseMatchMode.Fuzzy)) {
            aliases[key]?.let(normalized::get)?.let { return it }
        }
        if (options.exerciseMatchMode == ExerciseMatchMode.Fuzzy) {
            return catalog.asSequence()
                .filter { it.category == category }
                .map { it to nameSimilarity(key, normalizeExerciseName(it.name)) }
                .maxByOrNull(Pair<Exercise, Double>::second)
                ?.takeIf { it.second >= options.fuzzyMatchThreshold }
                ?.first
        }
        return null
    }
}

/** Lower case, letters and digits only, single spaces: "Bench-Press (Barbell)" is "bench press barbell". */
internal fun normalizeExerciseName(value: String): String = value.lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")

/** 0 to 1: mostly shared words, plus a little for a shared start. */
private fun nameSimilarity(left: String, right: String): Double {
    if (left == right) return 1.0
    val a = left.split(' ').filter(String::isNotBlank).toSet()
    val b = right.split(' ').filter(String::isNotBlank).toSet()
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val tokenScore = a.intersect(b).size.toDouble() / a.union(b).size
    val prefix = left.zip(right).takeWhile { it.first == it.second }.size.toDouble() /
        max(left.length, right.length).coerceAtLeast(1)
    return tokenScore * 0.85 + prefix * 0.15
}

private val BUILT_IN_EXERCISE_ALIASES = mapOf(
    "bench press" to "barbell bench press",
    "barbell squat" to "back squat",
    "cable seated row" to "seated cable row",
    "stationary bicycle" to "stationary bike",
    "running treadmill" to "treadmill run",
)
