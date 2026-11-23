package com.lukr99.workout.ui.i18n

/** Text on the live workout screen: top bar, exercise cards, empty start, toasts and set rows. */
object LiveWorkoutText : TextCatalog("live") {
    val defaultName = msg("Workout", "Trénink")
    val minimise = msg("Minimise workout", "Sbalit trénink")
    val finish = msg("Finish", "Dokončit")
    val discard = msg("Discard", "Zahodit")
    val nearPlace = msg("near {place}", "poblíž {place}")
    val whereTraining = msg("Where are you training?", "Kde dnes trénuješ?")
    val setHint = msg("Tap a set's number for tags, effort, a note, or to remove it.", "Klepni na číslo série pro štítky, námahu, poznámku nebo odebrání.")
    val addExercises = msg("Add exercises", "Přidat cviky")
    val setsCount = plural("{n} set", "{n} sets", "{n} série", "{n} série", "{n} sérií")
    val splitDetail = msg("{here} here, {other} at {place}", "{here} tady, {other}: {place}")

    val setLogged = msg("Set logged", "Série zapsána")
    val exerciseAdded = msg("{name} added", "{name} přidán")
    val workoutCopied = msg("Copied {name}", "Zkopírováno: {name}")
    val switchedTo = msg("Switched to {name}", "Přepnuto na {name}")
    val trackedPerGym = msg("{name} is tracked per gym", "{name} se teď sleduje zvlášť pro každou posilovnu")

    val prReps = msg(" reps", " opakování")
    val prE1rm = msg(" {unit} e1RM", " {unit} odhad 1RM")

    // Exercise card
    val supersetPosition = msg("SUPERSET · {n} OF {total}", "SUPERSERIE · {n} Z {total}")
    val edit = msg("Edit", "Upravit")
    val bestSet = msg("Best set {reps} × {weight}", "Nejlepší série {reps} × {weight}")
    val addSet = msg("Add set", "Přidat sérii")
    val addNote = msg("Add note", "Přidat poznámku")
    val reopenExercise = msg("Reopen exercise", "Znovu otevřít cvik")
    val startExercise = msg("Start exercise", "Začít cvik")
    val finishExercise = msg("Finish exercise", "Dokončit cvik")
    val statusFinished = msg("Finished · {time}", "Hotovo · {time}")
    val statusInProgress = msg("In progress · {time}", "Probíhá · {time}")
    val statusNotStarted = msg("Not started", "Nezačato")
    val expandFinished = msg("Expand finished exercise", "Rozbalit hotový cvik")
    val collapseFinished = msg("Collapse finished exercise", "Sbalit hotový cvik")
    val howToDo = msg("How to do {name}", "Jak provést cvik {name}")
    val moreFor = msg("More for {name}", "Další možnosti pro {name}")

    // Compact row and next set
    val superset = msg("Superset", "Superserie")
    val cardio = msg("Cardio", "Kardio")
    val setsPlanned = plural("{n} set planned", "{n} sets planned", "{n} série v plánu", "{n} série v plánu", "{n} sérií v plánu")
    val openName = msg("Open {name}", "Otevřít {name}")
    val nextSetOf = msg("set {n} of {total}", "série {n} z {total}")

    // Editing a set of a past workout
    val addSetNote = msg("Add note to set {n}", "Přidat poznámku k sérii {n}")
    val editSetNote = msg("Edit note on set {n}", "Upravit poznámku u série {n}")
    val removeSet = msg("Remove set", "Odebrat sérii")
    val setReps = msg("Set {n} · Reps", "Série {n} · Opakování")
    val setWeight = msg("Set {n} · Weight", "Série {n} · Váha")

    // Empty workout
    val searchOrType = msg("Search the library, or type a new name", "Hledej v knihovně nebo napiš nový název")
    val recentTapToAdd = msg("Recent, tap to add", "Nedávné, klepnutím přidáš")
    val addName = msg("Add {name}", "Přidat {name}")
    val repeatWorkout = msg("Repeat a workout", "Zopakovat trénink")
    val orSwitchTemplate = msg("Or switch to a template", "Nebo přepnout na šablonu")
    val switchToName = msg("Switch to {name}", "Přepnout na {name}")
    val copy = msg("Copy", "Kopírovat")
    val copyName = msg("Copy {name}", "Kopírovat {name}")
    val dayAndExercises = plural(
        "{day} · {n} exercise",
        "{day} · {n} exercises",
        "{day} · {n} cvik",
        "{day} · {n} cviky",
        "{day} · {n} cviků",
    )
}
