package com.lukr99.workout.ui.i18n

import androidx.compose.runtime.Composable

/**
 * One piece of user-facing text in every language. The Czech text is a required constructor
 * argument, so a message cannot exist without it. `{name}` marks a value that [text] fills in; both
 * languages must use the same names (`MessageCatalogTest` checks it).
 *
 * Create messages inside a [TextCatalog]. Ember writes the text once for Android and the web
 * edition, because the screens are shared code (ARCHITECTURE.md, "Shared code").
 */
class Msg internal constructor(val en: String, val cs: String) {

    /** The text in [language], with each `{name}` replaced by its value from [args]. */
    fun get(language: Language, vararg args: Pair<String, Any>): String =
        fill(if (language == Language.Czech) cs else en, args)

    /** The text in the screen's language. */
    @Composable
    fun text(vararg args: Pair<String, Any>): String = get(LocalLanguage.current, *args)

    /** The text in the app's current language, for code that is not a composable. */
    fun now(vararg args: Pair<String, Any>): String = get(AppLanguage.current, *args)
}

internal fun fill(template: String, args: Array<out Pair<String, Any>>): String {
    var out = template
    for ((name, value) in args) out = out.replace("{$name}", value.toString())
    return out
}

/** The `{name}` placeholders in [template], in the order they appear. */
internal fun placeholders(template: String): List<String> =
    Regex("""\{([A-Za-z0-9_]+)}""").findAll(template).map { it.groupValues[1] }.toList()
