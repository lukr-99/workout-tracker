package com.lukr99.workout.ui.i18n

/** Text for Run Mode: the live run, the route planner, the Runs tab, run detail and running progress. */
object RunText : TextCatalog("run") {
    // GPS line under "Start a run".
    val gpsDescribe = msg("GPS route, pace and splits", "GPS trasa, tempo a mezičasy")
    val gpsLooking = msg("Looking for GPS…", "Hledám GPS…")
    val gpsReady = msg("GPS ready", "GPS připravena")
    val gpsReadyAccuracy = msg("GPS ready · ±{m} m", "GPS připravena · ±{m} m")
    val gpsWeak = msg("Weak GPS · ±{m} m. Open sky helps.", "Slabé GPS · ±{m} m. Pomůže otevřené nebe.")

    // Live run card and screen.
    val time = msg("Time", "Čas")
    val pace = msg("Pace", "Tempo")
    val distance = msg("Distance", "Vzdálenost")
    val split = msg("Split {n}", "Mezičas {n}")
    val resume = msg("Resume", "Pokračovat")
    val pause = msg("Pause", "Pauza")
    val finish = msg("Finish", "Dokončit")
    val autoPaused = msg("Auto-paused", "Automaticky pozastaveno")
    val offRoute = msg("{m} m off route", "{m} m mimo trasu")
    val onRoute = msg("On route · {name}", "Na trase · {name}")
    val yourRoute = msg("your route", "tvoje trasa")
    val close = msg("Close", "Zavřít")
    val minimiseRun = msg("Minimise run", "Minimalizovat běh")
    val faceNorth = msg("Face north", "Sever nahoře")
    val lockToHeading = msg("Lock to heading", "Zamknout na směr")
    val recenter = msg("Recenter", "Vycentrovat")
    val lightMap = msg("Light map", "Světlá mapa")
    val darkMap = msg("Dark map", "Tmavá mapa")
    val startRunDescription = msg("Start run", "Spustit běh")
    val start = msg("Start", "Start", sameInCzech = true)
    val locationTitle = msg("Location for your run", "Poloha pro tvůj běh")
    val locationBody = msg(
        "Ember uses your location to draw your route and measure distance and pace. It only tracks while a run is active, never in the background.",
        "Ember používá tvoji polohu ke kreslení trasy a měření vzdálenosti a tempa. Sleduje ji jen při aktivním běhu, nikdy na pozadí.",
    )
    val enableLocation = msg("Enable location", "Povolit polohu")

    // Finish sheets.
    val runSaved = msg("Run saved", "Běh uložen")
    val newRecord = msg("New record · {label}", "Nový rekord · {label}")
    val finishRunQuestion = msg("Finish run?", "Dokončit běh?")
    val saveRun = msg("Save run", "Uložit běh")
    val keepRunning = msg("Keep running", "Běžet dál")
    val discard = msg("Discard", "Zahodit")
    val prFastest1k = msg("Fastest 1K", "Nejrychlejší 1 km")
    val prFastest5k = msg("Fastest 5K", "Nejrychlejší 5 km")
    val prFastest10k = msg("Fastest 10K", "Nejrychlejší 10 km")
    val prFastestHalf = msg("Fastest half marathon", "Nejrychlejší půlmaraton")
    val prLongestRun = msg("Longest run", "Nejdelší běh")
    val prMostElevation = msg("Most elevation", "Největší převýšení")
    val prBestAvgPace = msg("Best average pace", "Nejlepší průměrné tempo")

    // Route planner.
    val snapping = msg("Snapping to roads…", "Přichytávám k silnicím…")
    val tapToDrop = msg("Tap the map to drop waypoints", "Klepnutím do mapy přidáš body trasy")
    val tapAgain = msg("Tap again to draw a route", "Klepni znovu a nakreslí se trasa")
    val undo = msg("Undo", "Vrátit")
    val saveRoute = msg("Save route", "Uložit trasu")
    val routeSaved = msg("Route saved", "Trasa uložena")
    val routeSaveFailed = msg("Couldn't save route", "Trasu se nepodařilo uložit")
    val routeNamePlaceholder = msg("River loop", "Okruh kolem řeky")
    val routeDefaultName = msg("Route", "Trasa")

    // Run detail.
    val run = msg("Run", "Běh")
    val shareRun = msg("Share run", "Sdílet běh")
    val shareImage = msg("Share image", "Sdílet obrázek")
    val exportGpx = msg("Export GPX", "Exportovat GPX")
    val deleteRun = msg("Delete run", "Smazat běh")
    val plannedRoute = msg("Planned route · {name}", "Plánovaná trasa · {name}")
    val editNotes = msg("Edit notes", "Upravit poznámky")
    val movingTime = msg("Moving time", "Čas pohybu")
    val avgPace = msg("Avg pace", "Prům. tempo")
    val elevation = msg("Elevation", "Převýšení")
    val splits = msg("Splits", "Mezičasy")
    val loadingRun = msg("Loading run…", "Načítám běh…")
    val deleteRunQuestion = msg("Delete run?", "Smazat běh?")
    val deleteRunBody = msg("This permanently removes the run and its trace.", "Tím se běh i jeho stopa trvale odstraní.")
    val runNotes = msg("Run notes", "Poznámky k běhu")
    val runNotesPlaceholder = msg("Morning run, felt strong…", "Ranní běh, šlo to dobře…")

    // Running progress.
    val noRunsTitle = msg("No runs yet", "Zatím žádné běhy")
    val noRunsProgress = msg(
        "Record a run in the Runs tab. Your distance, pace trend and personal records show up here.",
        "Zaznamenej běh v záložce Běhy. Tady se ukáže tvoje vzdálenost, vývoj tempa a osobní rekordy.",
    )
    val totalDistance = msg("Total distance", "Celková vzdálenost")
    val runs = msg("Runs", "Běhy")
    val streak = msg("Streak", "Série")
    val weekUnit = msg("wk", "týd")
    val weeklyDistance = msg("Weekly distance", "Týdenní vzdálenost")
    val averagePace = msg("Average pace", "Průměrné tempo")
    val personalRecords = msg("Personal records", "Osobní rekordy")
    val progressHalf = msg("Fastest half", "Nejrychlejší půlmaraton")
    val progressBestAvg = msg("Best avg pace", "Nejlepší prům. tempo")

    // Runs tab.
    val weekSummary = msg("{distance} this week", "{distance} tento týden")
    val startRun = msg("Start a run", "Začít běh")
    val planRoute = msg("Plan a route", "Naplánovat trasu")
    val importGpx = msg("Import GPX", "Importovat GPX")
    val savedRoutes = msg("Saved routes", "Uložené trasy")
    val recentRuns = msg("Recent runs", "Poslední běhy")
    val noRunsHint = msg(
        "Tap Start a run above to record one with GPS, or import a GPX file from another app.",
        "Klepni nahoře na Začít běh a zaznamenej běh s GPS, nebo importuj soubor GPX z jiné aplikace.",
    )
    val deleteRouteQuestion = msg("Delete route?", "Smazat trasu?")
    val deleteRouteBody = msg("This removes “{name}” permanently.", "Trasa „{name}“ se trvale odstraní.")
    val renameRouteTitle = msg("Rename route", "Přejmenovat trasu")
    val renameRoutePlaceholder = msg("Riverside loop", "Okruh podél řeky")
    val rename = msg("Rename", "Přejmenovat")
    val saveOffline = msg("Save offline", "Uložit offline")
    val runRouteLabel = msg("Run {name}", "Běžet {name}")
    val thisRoute = msg("this route", "tuto trasu")
    val routeOptions = msg("Options for {name}", "Možnosti: {name}")
    val routeWord = msg("route", "trasa")

    // Status messages from the run view model.
    val importedRun = msg("Imported run · {km} km", "Importovaný běh · {km} km")
    val importFailed = msg("Couldn't import GPX: {reason}", "GPX se nepodařilo importovat: {reason}")
    val noTrackPoints = msg("No track points found", "Nenalezeny žádné body stopy")
    val noPathToCache = msg("This route has no path to cache.", "Tato trasa nemá žádnou cestu k uložení.")
    val cachingStart = msg("Caching map for offline…", "Ukládám mapu pro offline…")
    val cachingProgress = msg("Caching map… {percent}%", "Ukládám mapu… {percent} %")
    val cachingDone = msg("Map cached for offline use.", "Mapa je uložená pro offline použití.")
    val cachingFailed = msg("Offline caching failed: {reason}", "Uložení mapy offline selhalo: {reason}")
}
