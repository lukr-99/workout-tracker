package com.lukr99.workout.update

/**
 * Reads a published `.sha256` file. Accepts a bare digest or the `sha256sum` line
 * `<digest>  <file name>`; when a name is present it must be [expectedFileName].
 */
object Sha256File {
    private val LINE = Regex("^([0-9a-fA-F]{64})(?:\\s+\\*?(\\S+))?$")

    fun parse(text: String, expectedFileName: String): String {
        val line = text.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty)
            ?: error("The checksum file is empty.")
        val match = LINE.matchEntire(line) ?: error("The checksum file is not a SHA-256 digest.")
        val name = match.groupValues[2]
        check(name.isEmpty() || name == expectedFileName) {
            "The checksum file is for $name, not $expectedFileName."
        }
        return match.groupValues[1].lowercase()
    }
}
