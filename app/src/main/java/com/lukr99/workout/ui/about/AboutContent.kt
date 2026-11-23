package com.lukr99.workout.ui.about

import com.lukr99.workout.ui.i18n.AboutText
import com.lukr99.workout.ui.i18n.AppLanguage
import com.lukr99.workout.ui.i18n.Language

/**
 * The words on the About page that depend on the edition: what Ember keeps and where, and what it is
 * built from. Kept apart from the screen so tests can check them, and true to what the code does.
 * Screens pass their [Language] so they redraw when it changes; code and tests use the app's current one.
 */
object AboutContent {
    val TAGLINE: String get() = tagline()

    fun tagline(language: Language = AppLanguage.current): String = AboutText.tagline.get(language)

    /** One or two lines on what Ember is, for the hero. */
    fun summary(edition: AboutEdition, language: Language = AppLanguage.current): String = when (edition) {
        AboutEdition.Android -> AboutText.summaryAndroid.get(language)
        AboutEdition.Web -> AboutText.summaryWeb.get(language)
    }

    /** The edition's name in the facts row. */
    fun editionName(edition: AboutEdition, language: Language = AppLanguage.current): String = when (edition) {
        AboutEdition.Android -> AboutText.editionAndroid.get(language)
        AboutEdition.Web -> AboutText.editionWeb.get(language)
    }

    /** "Version 2.11.0", or just "Version" when the build did not say. */
    fun versionLabel(version: String, language: Language = AppLanguage.current): String =
        version.trim().removePrefix("v").let {
            if (it.isEmpty()) AboutText.version.get(language) else AboutText.versionWith.get(language, "version" to it)
        }

    /** Short points on what Ember keeps and where it goes, for the data and privacy card. */
    fun privacy(edition: AboutEdition, language: Language = AppLanguage.current): List<String> = when (edition) {
        AboutEdition.Android -> listOf(
            AboutText.privacyAndroid1, AboutText.privacyAndroid2, AboutText.privacyAndroid3, AboutText.privacyAndroid4,
        )
        AboutEdition.Web -> listOf(AboutText.privacyWeb1, AboutText.privacyWeb2, AboutText.privacyWeb3)
    }.map { it.get(language) }

    /** What Ember is made from, as this edition ships it. */
    fun credits(edition: AboutEdition, language: Language = AppLanguage.current): List<AboutCredit> = buildList {
        add(
            AboutCredit(
                AboutCreditKind.Source,
                "Ember",
                AboutText.creditEmberCredit.get(language, "name" to AuthorLinks.NAME),
                "PolyForm Noncommercial 1.0.0",
            ),
        )
        add(
            AboutCredit(
                AboutCreditKind.Type,
                AboutText.creditTypefaces.get(language),
                AboutText.creditTypefacesCredit.get(language),
                "SIL Open Font License 1.1",
            ),
        )
        add(
            AboutCredit(
                AboutCreditKind.Icons,
                AboutText.creditIcons.get(language),
                AboutText.creditIconsCredit.get(language),
                "Apache License 2.0",
            ),
        )
        add(
            AboutCredit(
                AboutCreditKind.Libraries,
                AboutText.creditBuiltWith.get(language),
                when (edition) {
                    AboutEdition.Android -> AboutText.creditBuiltWithAndroid.get(language)
                    AboutEdition.Web -> AboutText.creditBuiltWithWeb.get(language)
                },
                "Apache License 2.0",
            ),
        )
        if (edition == AboutEdition.Android) {
            add(
                AboutCredit(
                    AboutCreditKind.Pictures,
                    AboutText.creditPictures.get(language),
                    AboutText.creditPicturesCredit.get(language),
                    AboutText.creditPicturesLicence.get(language),
                ),
            )
            add(
                AboutCredit(
                    AboutCreditKind.Maps,
                    AboutText.creditMaps.get(language),
                    AboutText.creditMapsCredit.get(language),
                    AboutText.creditMapsLicence.get(language),
                ),
            )
        }
    }
}
