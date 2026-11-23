package com.lukr99.workout.ui.i18n

/** Text on the Home screen: the week card, templates and recent activity. */
object HomeText : TextCatalog("home") {
    val thisWeek = msg("This week", "Tento týden")
    val startFromTemplate = msg("Start from a template", "Začít ze šablony")
    val allTemplates = msg("All templates", "Všechny šablony")
    val recent = msg("Recent", "Nedávné")
    val emptyTitleBoth = msg("No workouts or runs yet", "Zatím žádné tréninky ani běhy")
    val emptyTitleWorkouts = msg("No workouts yet", "Zatím žádné tréninky")
    val emptyBodyBoth = msg(
        "Tap the orange Start button below to log a workout or record a run.",
        "Klepni na oranžové tlačítko Start dole a zapiš trénink nebo nahraj běh.",
    )
    val emptyBodyWorkouts = msg("Tap the orange Start button below to log a workout.", "Klepni na oranžové tlačítko Start dole a zapiš trénink.")
    val weekStreak = plural("{n} week streak", "{n} week streak", "{n} týden v řadě", "{n} týdny v řadě", "{n} týdnů v řadě")

    val inProgress = msg("IN PROGRESS · {n} MIN", "PROBÍHÁ · {n} MIN")
    val exercisesOf = plural(
        "{done} of {n} exercise",
        "{done} of {n} exercises",
        "{done} z {n} cviku",
        "{done} z {n} cviků",
        "{done} z {n} cviků",
    )
    val setsDone = plural("{n} set done", "{n} sets done", "{n} série hotová", "{n} série hotové", "{n} sérií hotovo")

    val totalWorkouts = plural("Workout", "Workouts", "Trénink", "Tréninky", "Tréninků")
    val totalLifted = msg("Lifted", "Zvednuto")
    val totalRun = msg("Run", "Běh")

    val dayWorkoutAndRun = msg("workout and run", "trénink a běh")
    val dayWorkout = msg("workout", "trénink")
    val dayRun = msg("run", "běh")
    val dayRest = msg("rest", "odpočinek")
    val dayToday = msg(", today", ", dnes")
    val chooseActivity = msg("Choose a workout or run", "Vybrat trénink nebo běh")
    val openRun = msg("Open run", "Otevřít běh")
    val openWorkout = msg("Open workout", "Otevřít trénink")

    val previewName = msg("Preview {name}", "Náhled: {name}")
    val startName = msg("Start {name}", "Začít: {name}")
    val noExercisesYet = msg("No exercises yet", "Zatím žádné cviky")
    val run = msg("Run", "Běh")
    val workout = msg("Workout", "Trénink")
}
