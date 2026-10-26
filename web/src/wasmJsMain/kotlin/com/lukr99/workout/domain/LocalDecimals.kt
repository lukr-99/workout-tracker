package com.lukr99.workout.domain

/**
 * [value] with [decimals] digits after the decimal point, like `"%.Nf".format(value)`.
 *
 * Each platform supplies this file. Android follows the phone's locale; the web edition always uses
 * a point, because a browser does not give the page a separator setting it can trust. The shared
 * [formatFixed] does the rounding the same way the JVM does.
 */
fun localDecimal(value: Double, decimals: Int): String = formatFixed(value, decimals)
