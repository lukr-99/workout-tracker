package com.lukr99.workout.ui.i18n

/** Text of the template editor, its exercise cards, the rep range sheet and the preview sheet. */
object TemplateEditorText : TextCatalog("templateEditor") {
    val created = msg("Template created", "Šablona vytvořena")
    val saved = msg("Template saved", "Šablona uložena")
    val couldNotSave = msg("Could not save", "Nepodařilo se uložit")
    val newTemplate = msg("New template", "Nová šablona")
    val editTemplate = msg("Edit template", "Upravit šablonu")
    val create = msg("Create", "Vytvořit")

    val nameLabel = msg("Name", "Název")
    val namePlaceholder = msg("Push day", "Den tlaku")
    val noteLabel = msg("Template note", "Poznámka k šabloně")
    val notePlaceholder = msg(
        "Heavy bench first. Short rests on the accessories.",
        "Nejdřív těžký bench. Krátké pauzy u doplňkových cviků.",
    )
    val exercisesHeader = msg("EXERCISES · {n}", "CVIKY · {n}")
    val holdDots = msg("Hold the dots to move", "Podrž tečky a přesuň")
    val addExercises = msg("Add exercises", "Přidat cviky")
    val footer = msg(
        "Changes here only affect new workouts. Logged workouts stay as they were.",
        "Změny se týkají jen nových tréninků. Zapsané tréninky zůstanou, jaké byly.",
    )
    val pickerSubtitle = msg(
        "They go to the end of the template with 3 sets each.",
        "Přidají se na konec šablony, každý se 3 sériemi.",
    )
    val rowNoteHint = msg(
        "Copied into the workout when you start this template, like \"pause on the chest\".",
        "Při spuštění šablony se zkopíruje do tréninku, třeba „pauza na hrudi“.",
    )

    // Exercise card
    val supersetBanner = msg("SUPERSET · DONE AS ONE ROUND", "SUPERSERIE · JEDNO KOLO")
    val ungroup = msg("Ungroup", "Rozpojit")
    val holdToMove = msg("Hold to move {name}", "Podrž a přesuň {name}")
    val setsLabel = msg("Sets", "Série")
    val setsLess = msg("Fewer sets", "Méně sérií")
    val setsMore = msg("More sets", "Více sérií")
    val setsSet = msg("Set the sets", "Nastavit série")
    val repsLabel = msg("Reps", "Opakování")
    val repsLess = msg("Fewer reps", "Méně opakování")
    val repsMore = msg("More reps", "Více opakování")
    val repsSet = msg("Set the reps", "Nastavit opakování")
    val restLabel = msg("Rest", "Odpočinek")
    val restLess = msg("Less rest", "Méně odpočinku")
    val restMore = msg("More rest", "Více odpočinku")
    val restSet = msg("Set the rest", "Nastavit odpočinek")
    val restAuto = msg("Auto", "Auto", sameInCzech = true)
    val moreFor = msg("More for {name}", "Více pro {name}")
    val addNote = msg("Add a note", "Přidat poznámku")
    val editNote = msg("Edit the note", "Upravit poznámku")
    val leaveSuperset = msg("Leave the superset", "Opustit superserii")
    val supersetWithPrevious = msg("Superset with previous", "Superserie s předchozím")
    val moveUp = msg("Move up", "Posunout nahoru")
    val moveDown = msg("Move down", "Posunout dolů")
    val remove = msg("Remove", "Odebrat")

    // Rep range sheet
    val repsFor = msg("Reps for {name}", "Opakování: {name}")
    val repRangeHint = msg(
        "Aim for the range; the workout starts each set at the top.",
        "Drž se rozsahu; trénink začíná každou sérii na horní hranici.",
    )
    val from = msg("From", "Od")
    val to = msg("To", "Do")
    val fewer = msg("{label} fewer", "{label}: méně")
    val more = msg("{label} more", "{label}: více")
    val noRange = msg("No range", "Bez rozsahu")

    // Superset editor sheet
    val editSuperset = msg("Edit superset", "Upravit superserii")
    val supersetHint = msg(
        "Exercises stay together and are performed as one round.",
        "Cviky zůstávají spolu a dělají se jako jedno kolo.",
    )
    val addBefore = msg("+ Add {name} before", "+ Přidat {name} před")
    val addAfter = msg("+ Add {name} after", "+ Přidat {name} za")
    val ungroupSuperset = msg("Ungroup superset", "Zrušit superserii")

    // Preview sheet
    val exerciseCount = plural("{n} exercise", "{n} exercises", "{n} cvik", "{n} cviky", "{n} cviků")
    val aboutMinutes = msg("about {n} min", "asi {n} min")
    val lastDone = msg("last done {when}", "naposledy {when}")
    val plan = msg("Plan", "Plán")
    val setsTimesReps = msg("Sets × reps", "Série × opakování")
    val edit = msg("Edit", "Upravit")
    val startWorkout = msg("Start workout", "Začít trénink")
    val lastSet = msg("Last {weight} × {reps}", "Minule {weight} × {reps}")
    val setCount = plural("{n} set", "{n} sets", "{n} série", "{n} série", "{n} sérií")
}
