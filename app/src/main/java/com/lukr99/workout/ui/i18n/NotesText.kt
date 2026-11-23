package com.lukr99.workout.ui.i18n

/** Text of the note lines, the note editor and the exercise guide sheet. */
object NotesText : TextCatalog("notes") {
    // Exercise guide sheet
    val yourNote = msg("Your note", "Tvoje poznámka")
    val howToDoIt = msg("How to do it", "Jak na to")
    val noSteps = msg(
        "No steps yet. Add them with Edit, one step per line.",
        "Zatím žádné kroky. Přidej je přes Upravit, jeden krok na řádek.",
    )
    val noAppForLink = msg("No app here can open this link", "Žádná aplikace tady tento odkaz neotevře")
    val openGuide = msg("Open guide", "Otevřít návod")
    val edit = msg("Edit", "Upravit")

    // Exercise notes panel
    val exerciseNote = msg("Exercise note", "Poznámka ke cviku")
    val machineNote = msg("Machine note for this gym", "Poznámka ke stroji v této posilovně")
    val lastTime = msg("Last time, {date}", "Minule, {date}")
    val noteFromLastTime = msg("Note from last time", "Poznámka z minula")
    val noteForWorkout = msg("Note for this workout", "Poznámka k tomuto tréninku")

    // Notes by gym
    val notesByGym = msg("Notes by gym", "Poznámky podle posilovny")
    val gymNotesHelp = msg(
        "Machine setup that differs between places, like the seat or which pulley. Shown while you log this exercise there.",
        "Nastavení stroje, které se liší podle místa, třeba sedadlo nebo kladka. Zobrazí se, když tam cvik zapisuješ.",
    )
    val editNoteFor = msg("Edit note for {name}", "Upravit poznámku pro {name}")
    val noNote = msg("No note", "Bez poznámky")
    val noteTitle = msg("Note · {name}", "Poznámka · {name}")
    val noteHint = msg(
        "Setup for {name} only. Leave it empty to remove the note.",
        "Nastavení jen pro {name}. Nech pole prázdné a poznámka se smaže.",
    )
    val notePlaceholder = msg("Double pulley, seat 4", "Dvojitá kladka, sedadlo 4")

    // Note editor and workout note row
    val deleteNote = msg("Delete note", "Smazat poznámku")
    val addWorkoutNotePrompt = msg("Add a note for this workout", "Přidej poznámku k tréninku")
    val addWorkoutNote = msg("Add workout note", "Přidat poznámku k tréninku")
    val workoutNoteEdit = msg("Workout note, tap to edit", "Poznámka k tréninku, klepnutím ji upravíš")
}
