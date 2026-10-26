package com.lukr99.workout.data.document

/**
 * Works around a Kotlin/Wasm 2.0 bug: `1e-7.toString()` gives ".e-7" (the leading digit is lost),
 * so kotlinx.serialization writes JSON that nothing can read back. A JSON number never starts with
 * a dot, so a dot that starts a number outside a string literal and is followed by an exponent is
 * this bug, and [repair] puts the "1" back. On the JVM the input never contains one and comes back
 * unchanged. Remove this once the web edition is on a Kotlin version with the fix.
 */
object JsonNumberRepair {

    fun repair(json: String): String {
        if (".e" !in json) return json
        val out = StringBuilder(json.length + 8)
        var inString = false
        var escaped = false
        var previous = ' '
        for ((index, char) in json.withIndex()) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else if (char == '"') {
                inString = true
            } else if (char == '.' && startsNumber(previous) && json.getOrNull(index + 1).let { it == 'e' || it == 'E' }) {
                // ".e-7" becomes "1e-7": the dot goes too, since "1.e-7" is not valid JSON either.
                out.append('1')
                previous = '1'
                continue
            }
            out.append(char)
            if (!char.isWhitespace()) previous = char
        }
        return out.toString()
    }

    private fun startsNumber(previous: Char) = previous == ':' || previous == ',' || previous == '[' || previous == '-'
}
