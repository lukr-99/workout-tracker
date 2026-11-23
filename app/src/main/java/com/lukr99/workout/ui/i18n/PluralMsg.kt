package com.lukr99.workout.ui.i18n

import androidx.compose.runtime.Composable

/**
 * A count with its noun. English has two forms, Czech has three: 1 (one), 2 to 4 (few) and
 * 0 or 5 and more (many). `{n}` is replaced by the count; other `{name}` values come from the args.
 */
class PluralMsg internal constructor(
    val enOne: String,
    val enOther: String,
    val csOne: String,
    val csFew: String,
    val csMany: String,
) {
    fun get(language: Language, n: Int, vararg args: Pair<String, Any>): String {
        val form = when (language) {
            Language.English -> if (n == 1) enOne else enOther
            Language.Czech -> when (n) {
                1 -> csOne
                in 2..4 -> csFew
                else -> csMany
            }
        }
        return fill(form, arrayOf("n" to n, *args))
    }

    @Composable
    fun text(n: Int, vararg args: Pair<String, Any>): String = get(LocalLanguage.current, n, *args)

    fun now(n: Int, vararg args: Pair<String, Any>): String = get(AppLanguage.current, n, *args)
}
