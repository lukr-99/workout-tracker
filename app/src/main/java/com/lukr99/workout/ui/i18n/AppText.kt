package com.lukr99.workout.ui.i18n

/** Text of the app shell (tabs, toasts, update status) and short summaries built outside a screen. */
object AppText : TextCatalog("app") {
    val tabHome = msg("Home", "Domů")
    val tabLibrary = msg("Library", "Knihovna")
    val tabRuns = msg("Runs", "Běhy")
    val tabProgress = msg("Progress", "Pokrok")

    val startWorkout = msg("Start workout", "Zahájit trénink")
    val resumeWorkout = msg("Resume workout", "Pokračovat v tréninku")
    val start = msg("Start", "Start", sameInCzech = true)
    val resume = msg("Resume", "Pokračovat")
    val done = msg("Done", "Hotovo")

    val locationAdded = msg("{name} added", "Místo {name} přidáno")
    val locationAddedMoved = plural(
        "{name} added, {n} workout moved there",
        "{name} added, {n} workouts moved there",
        "Místo {name} přidáno, {n} trénink přesunut sem",
        "Místo {name} přidáno, {n} tréninky přesunuty sem",
        "Místo {name} přidáno, {n} tréninků přesunuto sem",
    )

    val statsSets = plural("{n} set", "{n} sets", "{n} série", "{n} série", "{n} sérií")
    val statsReps = plural("{n} rep", "{n} reps", "{n} opakování", "{n} opakování", "{n} opakování")
    val statsElapsed = msg("{time} elapsed", "{time} celkem")
    val statsNoCardio = msg("No cardio stats", "Žádné statistiky kardia")

    val thisGym = msg("This gym", "Tato posilovna")
    val allGyms = msg("All gyms", "Všechny posilovny")
    val byGym = msg("By gym", "Podle posilovny")
    val anotherGym = msg("another gym", "jiná posilovna")
    val weekLabel = msg("W{n}", "T{n}")
    val templateCopy = msg("{name} copy", "{name} kopie")

    val operationFailed = msg("Operation failed.", "Operace se nezdařila.")
    val noImportPreview = msg("There is no import preview to commit.", "Není co importovat, chybí náhled importu.")
    val pickFileFirst = msg("Pick a file to import first.", "Nejdřív vyber soubor k importu.")

    val updateChecking = msg("Checking for updates…", "Hledám aktualizace…")
    val updateUpToDate = msg("You're on the latest version.", "Máš nejnovější verzi.")
    val updateDevBuild = msg(
        "This is Ember dev, a development build. It does not update itself.",
        "Tohle je Ember dev, vývojová verze. Sama se neaktualizuje.",
    )
    val updateAvailable = msg("Update available: v{version}", "Je dostupná aktualizace: v{version}")
    val updateNotVerifiable = msg(
        "v{version} is out but cannot be verified ({reason}) Download it from the releases page instead.",
        "Verze v{version} je venku, ale nejde ověřit ({reason}) Stáhni ji raději ze stránky s vydáními.",
    )
    val updateCheckFailed = msg("Check failed", "Kontrola selhala")
    val updateDownloading = msg("Downloading v{version}… {percent}%", "Stahuji v{version}… {percent} %")
    val updateOpeningInstaller = msg("Opening the installer…", "Otevírám instalátor…")
    val updateInstallerFailed = msg("Couldn't open the installer", "Instalátor se nepodařilo otevřít")
    val updateDownloadFailed = msg("Download failed", "Stahování selhalo")
}
