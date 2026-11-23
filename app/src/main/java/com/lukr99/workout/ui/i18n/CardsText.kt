package com.lukr99.workout.ui.i18n

/** Text of the small shared cards, sheets and dialogs in `ui/components`: start chooser, update card, music, dialogs. */
object CardsText : TextCatalog("cards") {
    val close = msg("Close", "Zavřít")
    val run = msg("Run", "Běh")
    val workout = msg("Workout", "Trénink")
    val openActivity = msg("Open {name}", "Otevřít {name}")

    val previous = msg("Previous", "Předchozí")
    val pause = msg("Pause", "Pozastavit")
    val play = msg("Play", "Přehrát")
    val next = msg("Next", "Další")
    val openSpotify = msg("Open Spotify", "Otevřít Spotify")

    val decrease = msg("Decrease", "Snížit")
    val increase = msg("Increase", "Zvýšit")
    val clearSearch = msg("Clear search", "Smazat hledání")
    val typeToConfirm = msg("Type \"{word}\" to confirm", "Pro potvrzení napiš „{word}“")

    val start = msg("Start", "Začít")
    val lift = msg("Lift", "Posilování")
    val logStrength = msg("Log a strength workout", "Zapiš silový trénink")
    val trackRun = msg("Track a run with GPS + map", "Sleduj běh přes GPS a mapu")
    val resume = msg("Resume", "Pokračovat")
    val backToWorkout = msg("Back to your live workout", "Zpět k probíhajícímu tréninku")
    val backToRun = msg("Back to your live run", "Zpět k probíhajícímu běhu")

    val updateReady = msg("Ember {version} is ready", "Ember {version} je připravený")
    val updateBody = msg(
        "A signed update from GitHub. Your data stays as it is.",
        "Podepsaná aktualizace z GitHubu. Tvoje data zůstanou beze změny.",
    )
    val later = msg("Later", "Později")
    val update = msg("Update", "Aktualizovat")
    val gotIt = msg("Got it", "Rozumím")
    val hideWhatsNew = msg("Hide what's new", "Skrýt novinky")
}
