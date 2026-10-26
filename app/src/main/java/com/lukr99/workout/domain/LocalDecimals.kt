package com.lukr99.workout.domain

/**
 * [value] with [decimals] digits after the device's decimal separator, like `"%.Nf".format(value)`.
 *
 * Each platform supplies this file. On Android it follows the phone's locale, as the app always
 * has; the web edition's version is under `web/`.
 */
fun localDecimal(value: Double, decimals: Int): String = "%.${decimals}f".format(value)
