package com.lukr99.workout.update

/** Which versions count as newer, and which builds may update themselves at all. */
object VersionPolicy {

    /** True for a debug or test build's version, which carries the `-dev` suffix. */
    fun isDevelopmentVersion(version: String): Boolean =
        version.trim().endsWith("-dev", ignoreCase = true)

    /** True when [candidate] is a strictly higher dotted-numeric version than [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        val c = parse(candidate)
        val cur = parse(current)
        for (i in 0 until maxOf(c.size, cur.size)) {
            val a = c.getOrElse(i) { 0 }
            val b = cur.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").split('.', '-')
            .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
}
