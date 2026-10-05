package com.lukr99.workout.ui.components

/**
 * What the set number pad shows and how keys change it. Weight is in the display unit (kg or lb).
 *
 * The first key after picking a field replaces its value, so typing "8" over "10" gives 8, not 108.
 * Every later key appends. A step (+2.5, -1) changes the value and starts fresh typing again.
 */
data class SetEntryState(
    val field: SetField = SetField.Weight,
    val weight: String = "0",
    val reps: String = "0",
    val fresh: Boolean = true,
) {
    val weightValue: Double get() = weight.toDoubleOrNull() ?: 0.0
    val repsValue: Int get() = reps.toIntOrNull() ?: 0

    fun select(next: SetField): SetEntryState = copy(field = next, fresh = true)

    /** A digit, "." or [BACKSPACE]. */
    fun press(key: String): SetEntryState {
        val current = if (field == SetField.Weight) weight else reps
        val next = when (key) {
            BACKSPACE -> if (fresh) "0" else current.dropLast(1).ifEmpty { "0" }
            "." -> when {
                field == SetField.Reps -> return this
                fresh -> "0."
                '.' in current -> return this
                else -> "$current."
            }
            else -> when {
                fresh || current == "0" -> key
                current.length >= MAX_LENGTH -> return this
                '.' in current && current.substringAfter('.').isNotEmpty() -> return this
                else -> current + key
            }
        }
        return set(next).copy(fresh = false)
    }

    fun step(delta: Double): SetEntryState {
        val value = ((if (field == SetField.Weight) weightValue else repsValue.toDouble()) + delta).coerceAtLeast(0.0)
        return set(if (field == SetField.Weight) trim(value) else value.toInt().toString()).copy(fresh = true)
    }

    /** Copies another set's values, for "same as the set before". */
    fun copyFrom(weightDisplay: Double, repsCount: Int): SetEntryState =
        copy(weight = trim(weightDisplay), reps = repsCount.toString(), fresh = true)

    private fun set(text: String) = if (field == SetField.Weight) copy(weight = text) else copy(reps = text)

    companion object {
        const val BACKSPACE = "back"
        private const val MAX_LENGTH = 5

        fun of(weightDisplay: Double, reps: Int, field: SetField = SetField.Weight) =
            SetEntryState(field = field, weight = trim(weightDisplay), reps = reps.toString())

        /** One decimal at most, and no ".0": 82.5, 80. */
        fun trim(value: Double): String {
            val rounded = Math.round(value * 10.0) / 10.0
            return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
        }
    }
}
