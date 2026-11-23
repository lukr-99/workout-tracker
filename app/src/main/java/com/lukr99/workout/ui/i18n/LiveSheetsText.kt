package com.lukr99.workout.ui.i18n

/** Text on the sheets over the live workout: exercise menu, finish sheet, pickers and dialogs. */
object LiveSheetsText : TextCatalog("live_sheets") {
    // Exercise menu
    val lastThreeTimes = msg("LAST 3 TIMES", "POSLEDNÍ 3 TRÉNINKY")
    val loading = msg("Loading…", "Načítám…")
    val firstTime = msg("First time. Nothing logged yet.", "Poprvé. Zatím nic nezapsáno.")
    val rightColumn = msg("Right column: best estimated 1RM ({unit})", "Pravý sloupec: nejlepší odhad 1RM ({unit})")
    val fullHistory = msg("Full history", "Celá historie")
    val replaceExercise = msg("Replace exercise", "Nahradit cvik")
    val replaceHint = msg("Machine taken? Swap it and keep your logged sets.", "Stroj je obsazený? Vyměň cvik a zapsané série zůstanou.")
    val leaveSuperset = msg("Leave the superset", "Opustit superserii")
    val leaveSupersetHint = msg("Do it on its own again.", "Dělej ho zase samostatně.")
    val supersetNext = msg("Superset with next", "Superserie s dalším")
    val supersetPrevious = msg("Superset with previous", "Superserie s předchozím")
    val pairWith = msg("Pair it with {name}.", "Spáruj ho s cvikem {name}.")
    val moveUp = msg("Move up", "Posunout nahoru")
    val moveDown = msg("Move down", "Posunout dolů")
    val addNoteToday = msg("Add a note for today", "Přidat poznámku pro dnešek")
    val editNoteToday = msg("Edit today's note", "Upravit dnešní poznámku")
    val editGymNote = msg("Edit the gym note", "Upravit poznámku k posilovně")
    val addGymNote = msg("Add a gym note", "Přidat poznámku k posilovně")
    val gymNoteHint = msg("Seat, pulley or setup at {place}. Kept for next time.", "Sedadlo, kladka nebo nastavení: {place}. Uchová se na příště.")
    val thisGym = msg("this gym", "tato posilovna")
    val howToDoIt = msg("How to do it", "Jak ho provést")
    val howToDoItHint = msg("Steps, your note and the guide link.", "Postup, tvoje poznámka a odkaz na návod.")
    val removeFromWorkout = msg("Remove from workout", "Odebrat z tréninku")
    val removeDoneSets = plural(
        "Its {n} done set goes too.",
        "Its {n} done sets go too.",
        "Smaže se i {n} hotová série.",
        "Smažou se i {n} hotové série.",
        "Smaže se i {n} hotových sérií.",
    )

    // Finish sheet
    val finishTitle = msg("Finish workout?", "Dokončit trénink?")
    val statTime = msg("Time", "Čas")
    val statLifted = msg("Lifted", "Zvednuto")
    val statSets = msg("Sets", "Série")
    val statPrs = msg("PRs", "OR")
    val notTicked = plural(
        "{n} SET NOT TICKED",
        "{n} SETS NOT TICKED",
        "{n} SÉRIE NEODŠKRTNUTÁ",
        "{n} SÉRIE NEODŠKRTNUTÉ",
        "{n} SÉRIÍ NEODŠKRTNUTO",
    )
    val leaveOut = msg("Leave them out", "Vynechat je")
    val leaveOutHint = msg("Only sets you ticked are saved.", "Uloží se jen série, které jsi odškrtl.")
    val countAsDone = msg("Count them as done", "Počítat je jako hotové")
    val countAsDoneHint = msg("Sets with reps are saved as done. Empty ones go.", "Série s opakováními se uloží jako hotové. Prázdné se zahodí.")
    val youChanged = msg("YOU CHANGED {name}", "ZMĚNY V ŠABLONĚ {name}")
    val changeAdded = msg("Added {name}", "Přidáno: {name}")
    val changeSkipped = msg("Skipped {name}", "Vynecháno: {name}")
    val changeSetCount = plural(
        "{name}: {n} set, the template has {planned}",
        "{name}: {n} sets, the template has {planned}",
        "{name}: {n} série, šablona jich má {planned}",
        "{name}: {n} série, šablona jich má {planned}",
        "{name}: {n} sérií, šablona jich má {planned}",
    )
    val updateTemplate = msg("Update {name}", "Aktualizovat {name}")
    val updateTemplateHint = msg("Next time starts the way you just did it.", "Příště začneš tak, jak jsi to teď udělal.")
    val keepTemplate = msg("Keep {name} as it is", "Nechat {name} beze změny")
    val keepTemplateHint = msg("Only this workout has the changes.", "Změny bude mít jen tento trénink.")
    val saveAsNew = msg("Save as a new template", "Uložit jako novou šablonu")
    val saveAsNewHint = msg("Keeps {name} and adds a copy with the changes.", "Ponechá {name} a přidá kopii se změnami.")
    val newTemplateName = msg("New template name", "Název nové šablony")
    val copySuffix = msg("copy", "kopie")
    val keepGoing = msg("Keep going", "Pokračovat")
    val finish = msg("Finish", "Dokončit")

    // Pickers, notes and dialogs
    val pickerSubtitle = msg("Pick one or more. They go to the end of the workout.", "Vyber jeden nebo více. Přidají se na konec tréninku.")
    val lastWeightReps = msg("Last {weight} × {reps}", "Naposledy {weight} × {reps}")
    val oneExerciseAdded = msg("{name} added", "{name} přidán")
    val exercisesAdded = plural(
        "{n} exercise added",
        "{n} exercises added",
        "Přidán {n} cvik",
        "Přidány {n} cviky",
        "Přidáno {n} cviků",
    )
    val createdAndAdded = msg("{name} created and added", "{name} vytvořen a přidán")
    val replaceTitle = msg("Replace {name}", "Nahradit {name}")
    val swappedFor = msg("Swapped for {name}", "Nahrazeno cvikem {name}")
    val gymNoteTitle = msg("Gym note · {name}", "Poznámka k posilovně · {name}")
    val gymNoteSheetHint = msg(
        "Setup for {place} only, like \"double pulley, seat 4\". It shows on this exercise every time you train here.",
        "Nastavení jen pro {place}, třeba „dvojitá kladka, sedadlo 4“. Zobrazí se u tohoto cviku pokaždé, když tu trénuješ.",
    )
    val gymNotePlaceholder = msg("Double pulley, seat 4", "Dvojitá kladka, sedadlo 4")
    val workoutSaved = msg("Workout saved", "Trénink uložen")
    val workoutSavedUpdated = msg("Workout saved, {name} updated", "Trénink uložen, {name} aktualizována")
    val workoutSavedNewTemplate = msg("Workout saved as a new template too", "Trénink uložen i jako nová šablona")
    val discardTitle = msg("Discard workout?", "Zahodit trénink?")
    val discardMessage = msg("This session will not be saved to your history.", "Tento trénink se neuloží do historie.")
    val discardConfirm = msg("Discard", "Zahodit")
}
