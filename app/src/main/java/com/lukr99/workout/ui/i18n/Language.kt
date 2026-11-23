package com.lukr99.workout.ui.i18n

import com.lukr99.workout.settings.LanguageSetting

/** A language Ember has text for. [code] is the BCP 47 language tag. */
enum class Language(val code: String) {
    English("en"),
    Czech("cs"),
    ;

    companion object {
        /** The language a setting means, given what the device itself uses. */
        fun resolve(setting: LanguageSetting, system: Language): Language = when (setting) {
            LanguageSetting.System -> system
            LanguageSetting.English -> English
            LanguageSetting.Czech -> Czech
        }

        /** The language for a device language tag like "cs", "cs-CZ" or "de". Anything unknown is English. */
        fun fromTag(tag: String): Language {
            val base = tag.substringBefore('-').substringBefore('_').lowercase()
            return entries.firstOrNull { it.code == base } ?: English
        }
    }
}
