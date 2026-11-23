package com.lukr99.workout.ui.i18n

/**
 * A group of messages for one area of the app, such as Settings. Subclass it as an `object`, add
 * every message with [msg] or [plural], and list the object in [AllCatalogs] so the translation
 * check sees it.
 */
abstract class TextCatalog(val catalogName: String) {
    internal class Entry(val msg: Msg?, val plural: PluralMsg?, val sameInCzech: Boolean)

    internal val entries = mutableListOf<Entry>()

    /**
     * A message. Set [sameInCzech] only when the Czech text is truly the same word, like "OK" or
     * "kg"; the check otherwise treats an identical Czech text as a missed translation.
     */
    protected fun msg(en: String, cs: String, sameInCzech: Boolean = false): Msg =
        Msg(en, cs).also { entries += Entry(it, null, sameInCzech) }

    /** A count with its noun: English one and other, Czech one, few and many. */
    protected fun plural(enOne: String, enOther: String, csOne: String, csFew: String, csMany: String): PluralMsg =
        PluralMsg(enOne, enOther, csOne, csFew, csMany).also { entries += Entry(null, it, false) }
}
