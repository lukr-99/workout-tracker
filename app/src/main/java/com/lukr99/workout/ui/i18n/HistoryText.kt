package com.lukr99.workout.ui.i18n

/** Text on the History list: search, selecting workouts and moving them to a place. */
object HistoryText : TextCatalog("history") {
    val searchPlaceholder = msg("Search workouts", "Hledat tréninky")
    val emptyTitle = msg("No workouts yet", "Zatím žádné tréninky")
    val emptyBody = msg(
        "Workouts you finish show up here, newest first. Tap Start below to log one.",
        "Dokončené tréninky se ukážou tady, nejnovější nahoře. Klepni na Start dole a zapiš první.",
    )
    val noMatchTitle = msg("No workouts match “{query}”", "Žádný trénink neodpovídá „{query}“")
    val noMatchBody = msg("Search looks at workout names. Try a shorter word.", "Hledá se v názvech tréninků. Zkus kratší slovo.")
    val moveTitle = plural("Move {n} workout to", "Move {n} workouts to", "Přesunout {n} trénink do", "Přesunout {n} tréninky do", "Přesunout {n} tréninků do")
    val movedTo = plural(
        "Moved {n} workout to {place}",
        "Moved {n} workouts to {place}",
        "Přesunut {n} trénink: {place}",
        "Přesunuty {n} tréninky: {place}",
        "Přesunuto {n} tréninků: {place}",
    )
    val movedToGym = msg("Moved {n} to {place}", "Přesunuto {n}: {place}")
    val stopSelecting = msg("Stop selecting", "Zrušit výběr")
    val selectedCount = msg("{n} selected", "Vybráno: {n}")
    val all = msg("All", "Vše")
    val moveToLocation = msg("Move to location", "Přesunout na místo")
    val select = msg("Select", "Vybrat")
    val selected = msg("Selected", "Vybráno")
    val exercisesLabel = plural("exercise", "exercises", "cvik", "cviky", "cviků")
    val setsLabel = plural("set", "sets", "série", "série", "sérií")
    val cardioMin = msg("cardio min", "min kardia")
}
