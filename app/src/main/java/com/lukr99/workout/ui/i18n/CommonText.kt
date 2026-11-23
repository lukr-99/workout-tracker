package com.lukr99.workout.ui.i18n

/** Words used on many screens: buttons, short labels and relative days. */
object CommonText : TextCatalog("common") {
    val back = msg("Back", "Zpět")
    val cancel = msg("Cancel", "Zrušit")
    val save = msg("Save", "Uložit")
    val delete = msg("Delete", "Smazat")
    val done = msg("Done", "Hotovo")
    val ok = msg("OK", "OK", sameInCzech = true)
    val today = msg("Today", "Dnes")
    val yesterday = msg("Yesterday", "Včera")
    val daysAgo = plural("{n} day ago", "{n} days ago", "před {n} dnem", "před {n} dny", "před {n} dny")
}
