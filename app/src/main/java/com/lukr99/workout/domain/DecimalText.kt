package com.lukr99.workout.domain

import kotlin.math.PI
import kotlin.math.abs

/*
 * Number formatting without java.util.Formatter, which the web edition does not have.
 */

/**
 * [value] with exactly [decimals] digits after a `.`, rounded half up, like
 * `String.format(Locale.ROOT, "%.Nf", value)`. Like the JVM, it rounds the shortest decimal form of
 * the double (so 2.675 gives "2.68"), and keeps the minus sign of a negative value that rounds to 0.
 */
fun formatFixed(value: Double, decimals: Int): String {
    require(decimals in 0..15) { "decimals must be 0..15" }
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    val negative = value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)
    val plain = plainDecimal(abs(value))
    val point = plain.indexOf('.')
    val whole = if (point < 0) plain else plain.substring(0, point)
    val fraction = (if (point < 0) "" else plain.substring(point + 1)).padEnd(decimals + 1, '0')
    var digits = whole + fraction.substring(0, decimals)
    if (fraction[decimals] >= '5') digits = incrementDigits(digits)
    val intPart = digits.substring(0, digits.length - decimals).trimStart('0').ifEmpty { "0" }
    val text = if (decimals == 0) intPart else intPart + "." + digits.substring(digits.length - decimals)
    return if (negative) "-$text" else text
}

/** [value] as a whole number padded with zeros to [width] digits, like `"%0Nd".format(value)`. */
fun padded(value: Int, width: Int): String =
    if (value < 0) "-" + (-value).toString().padStart(width - 1, '0') else value.toString().padStart(width, '0')

/** Degrees to radians, the same constant as `Math.toRadians`. */
fun Double.toRadians(): Double = this * DEGREES_TO_RADIANS

private const val DEGREES_TO_RADIANS = PI / 180.0

/** The shortest decimal form of a non-negative finite double, never in exponent notation. */
private fun plainDecimal(value: Double): String {
    val text = value.toString()
    val e = text.indexOfFirst { it == 'E' || it == 'e' }
    if (e < 0) return text
    val mantissa = text.substring(0, e)
    val exponent = text.substring(e + 1).toInt()
    val point = mantissa.indexOf('.')
    val digits = mantissa.replace(".", "")
    val pointAt = (if (point < 0) mantissa.length else point) + exponent
    return when {
        pointAt <= 0 -> "0." + "0".repeat(-pointAt) + digits
        pointAt >= digits.length -> digits + "0".repeat(pointAt - digits.length)
        else -> digits.substring(0, pointAt) + "." + digits.substring(pointAt)
    }
}

/** Adds one to a string of decimal digits, growing it on a carry ("99" gives "100"). */
private fun incrementDigits(digits: String): String {
    val chars = digits.toCharArray()
    var i = chars.size - 1
    while (i >= 0) {
        if (chars[i] == '9') {
            chars[i] = '0'
            i--
        } else {
            chars[i] = chars[i] + 1
            return chars.concatToString()
        }
    }
    return "1" + chars.concatToString()
}
