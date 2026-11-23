package com.lukr99.workout.ui.i18n

/** Text of the Progress hub, the Progress list and the per-exercise detail. */
object ProgressScreenText : TextCatalog("progressScreen") {
    val hubTitle = msg("Progress", "Pokrok")
    val hubSubtitle = msg("How your training is going", "Jak ti jde trénink")
    val paneProgress = msg("Progress", "Pokrok")
    val paneRunning = msg("Running", "Běh")
    val paneHistory = msg("History", "Historie")

    val loading = msg("Loading…", "Načítám…")
    val workouts = msg("Workouts", "Tréninky")
    val thisMonth = msg("+{n} this month", "+{n} tento měsíc")
    val noneThisMonth = msg("None this month", "Tento měsíc žádný")
    val volume4Weeks = msg("Volume, 4 wk", "Objem, 4 týd.")
    val volumeVsBefore = msg("{change}% vs 4 wk before", "{change} % oproti předchozím 4 týd.")
    val last4Weeks = msg("last 4 weeks", "poslední 4 týdny")
    val prSets = msg("PR sets", "Série s OR")
    val last30Days = msg("last 30 days", "posledních 30 dnů")
    val streak = msg("Streak", "Série týdnů")
    val weekShort = msg("wk", "týd.")
    val bestStreak = msg("best {n} wk", "nejlepší {n} týd.")
    val weeklyVolume = msg("Weekly volume", "Týdenní objem")
    val volumeThisWeek = msg("{value} {unit} this week", "{value} {unit} tento týden")
    val exercises = msg("Exercises", "Cviky")
    val emptyTitle = msg("No progress yet", "Zatím žádný pokrok")
    val emptyBody = msg(
        "Finish a workout with a few sets. Each exercise you log gets a chart of its best sets here.",
        "Dokonči trénink s pár sériemi. Každý cvik, který zapíšeš, tu dostane graf svých nejlepších sérií.",
    )
    val noWorkoutsAtGym = msg("No workouts at this gym yet", "Na této posilovně zatím žádný trénink")
    val estimated1Rm = msg("Est 1RM", "Odhad 1RM")

    // Exercise detail
    val exerciseFallback = msg("Exercise", "Cvik")
    val noSetsTitle = msg("No sets logged yet", "Zatím žádné série")
    val noSetsBody = msg(
        "Log this exercise in a workout and its chart and records show up here.",
        "Zapiš tento cvik v tréninku a jeho graf a rekordy se objeví tady.",
    )
    val trackedPerGym = msg("Tracked per gym. Showing {place} only.", "Sleduje se podle místa. Zobrazuje se jen {place}.")
    val noneHere = msg(
        "No workouts at {place} yet. Here is every gym.",
        "Na místě {place} zatím žádný trénink. Zde jsou všechna místa.",
    )
    val bestE1rmTile = msg("Best e1RM", "Nejlepší 1RM")
    val latestE1rmTile = msg("Latest e1RM", "Poslední 1RM")
    val sessionsTile = msg("Sessions", "Tréninky")
    val estimated1RmTitle = msg("Estimated 1RM", "Odhad 1RM")
    val volumePerSession = msg("Volume per session", "Objem za trénink")
    val chartHint = msg("Tap or drag along the chart to see each workout.", "Klepni nebo táhni po grafu a uvidíš každý trénink.")
    val byGymTitle = msg("Estimated 1RM by gym", "Odhad 1RM podle místa")
    val byGymSummaryIntro = msg("Estimated 1RM by gym.", "Odhad 1RM podle místa.")
    val byGymSummaryItem = plural(
        "{name}: {n} workout, latest {latest} {unit}, best {best} {unit}.",
        "{name}: {n} workouts, latest {latest} {unit}, best {best} {unit}.",
        "{name}: {n} trénink, poslední {latest} {unit}, nejlepší {best} {unit}.",
        "{name}: {n} tréninky, poslední {latest} {unit}, nejlepší {best} {unit}.",
        "{name}: {n} tréninků, poslední {latest} {unit}, nejlepší {best} {unit}.",
    )
    val compareGymsHint = msg("Tap or drag along the chart to compare gyms on a date.", "Klepni nebo táhni po grafu a porovnáš místa v daný den.")

    // Records
    val records = msg("Records", "Rekordy")
    val prBadge = msg("PR", "OR")
    val mostReps = msg("Most reps", "Nejvíce opakování")
    val repsCount = plural("{n} rep", "{n} reps", "{n} opakování", "{n} opakování", "{n} opakování")
    val heaviestSet = msg("Heaviest set", "Nejtěžší série")
    val bestE1rm = msg("Best e1RM", "Nejlepší odhad 1RM")
    val bestSetVolume = msg("Best set volume", "Nejlepší objem série")
    val bestSessionVolume = msg("Best session volume", "Nejlepší objem tréninku")
    val setsCount = plural("{n} set", "{n} sets", "{n} série", "{n} série", "{n} sérií")
    val repMaxes = msg("Rep maxes", "Maxima podle opakování")
}
