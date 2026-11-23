package com.lukr99.workout.ui.i18n

/** Text on the Data screen: import, export, the import preview and the danger zone. */
object DataText : TextCatalog("data") {
    val title = msg("Data", "Data", sameInCzech = true)
    val intro = msg(
        "Save JSON is a full backup: workouts, runs, routes, templates, exercises, photos and settings. Keep it somewhere outside the phone. CSV is a spreadsheet of your workouts and cannot restore anything. Every import shows a preview before it changes anything.",
        "Uložit JSON je úplná záloha: tréninky, běhy, trasy, šablony, cviky, fotky a nastavení. Ulož ji mimo telefon. CSV je tabulka tvých tréninků a nic z ní obnovit nejde. Každý import ti nejdřív ukáže náhled, než cokoli změní.",
    )
    val importFile = msg("Import file", "Importovat soubor")
    val saveJson = msg("Save JSON", "Uložit JSON")
    val saveCsv = msg("Save CSV", "Uložit CSV")
    val shareJson = msg("Share JSON", "Sdílet JSON")
    val shareCsv = msg("Share CSV", "Sdílet CSV")
    val shareBackupTitle = msg("Share workout backup", "Sdílet zálohu tréninků")
    val shareCsvTitle = msg("Share workout CSV", "Sdílet CSV tréninků")
    val operationFailed = msg("Operation failed.", "Operace selhala.")
    val allDeleted = msg("All data deleted. Ember is back to a fresh start.", "Všechna data jsou smazána. Ember je jako nově nainstalovaný.")

    val workoutsCount = plural("{n} workout", "{n} workouts", "{n} trénink", "{n} tréninky", "{n} tréninků")
    val runsCount = plural("{n} run", "{n} runs", "{n} běh", "{n} běhy", "{n} běhů")
    val routesCount = plural("{n} route", "{n} routes", "{n} trasa", "{n} trasy", "{n} tras")
    val photosCount = plural("{n} photo", "{n} photos", "{n} fotka", "{n} fotky", "{n} fotek")
    val templatesCount = plural("{n} template", "{n} templates", "{n} šablona", "{n} šablony", "{n} šablon")
    val exercisesCount = plural("{n} exercise", "{n} exercises", "{n} cvik", "{n} cviky", "{n} cviků")
    val newExercisesCount = plural("{n} new exercise", "{n} new exercises", "{n} nový cvik", "{n} nové cviky", "{n} nových cviků")
    val setsAndActivities = plural("{n} set or activity", "{n} sets and activities", "{n} série či aktivita", "{n} série či aktivity", "{n} sérií či aktivit")
    val yourSettings = msg("your settings", "tvoje nastavení")
    val replacedMessage = msg("Replaced your data with the backup: {restored}.", "Data byla nahrazena zálohou: {restored}.")
    val importedMessage = msg("Imported {restored}.", "Importováno: {restored}.")
    val alreadyHere = plural(
        "{n} workout was already here.",
        "{n} workouts were already here.",
        "{n} trénink tu už byl.",
        "{n} tréninky tu už byly.",
        "{n} tréninků tu už bylo.",
    )

    val dangerZone = msg("Danger zone", "Nebezpečná zóna")
    val deleteAll = msg("Delete all data", "Smazat všechna data")
    val deleteAllHint = msg(
        "Removes every workout, run, route, template, custom exercise, photo and setting, and turns off automatic backup. Your backup files stay where they are. Data already in Health Connect stays there.",
        "Odstraní všechny tréninky, běhy, trasy, šablony, vlastní cviky, fotky a nastavení a vypne automatickou zálohu. Soubory záloh zůstanou, kde jsou. Data, která už jsou v Health Connect, tam zůstanou.",
    )
    val deleteAllDialogTitle = msg("Delete all data?", "Smazat všechna data?")
    val deleteAllDialogMessage = msg(
        "Ember goes back to a fresh install. This cannot be undone.",
        "Ember se vrátí do stavu po čisté instalaci. Tohle nejde vrátit zpět.",
    )
    val deleteWord = msg("delete", "smazat")
    val deleteEverything = msg("Delete everything", "Smazat vše")

    val previewTitle = msg("Import preview", "Náhled importu")
    val emberBackupFrom = msg("Ember {version} backup from {date}", "Záloha Emberu {version} z {date}")
    val backupFrom = msg("Backup from {date}", "Záloha z {date}")
    val sessionsNewChanged = msg(
        "{added} new · {changed} changed · {skipped} already here",
        "Nové: {added} · změněné: {changed} · už tu jsou: {skipped}",
    )
    val merge = msg("Merge", "Sloučit")
    val replaceEverything = msg("Replace everything", "Nahradit vše")
    val replaceHint = msg(
        "Deletes everything on this phone first, then restores this backup exactly as it was saved, settings and photos included.",
        "Nejdřív smaže vše v tomto telefonu, pak obnoví tuhle zálohu přesně tak, jak byla uložena, včetně nastavení a fotek.",
    )
    val mergeHint = msg(
        "Adds what is new and merges what matches. Nothing on this phone is removed.",
        "Přidá, co je nové, a sloučí, co se shoduje. Nic se v telefonu nesmaže.",
    )
    val replaceMyData = msg("Replace my data", "Nahradit moje data")
    val importButton = msg("Import", "Importovat")
    val replaceDialogTitle = msg("Replace all your data?", "Nahradit všechna tvoje data?")
    val replaceDialogMessage = msg(
        "The backup takes their place. This cannot be undone. Save a backup first if you are not sure.",
        "Záloha je nahradí. Tohle nejde vrátit zpět. Pokud váháš, nejdřív ulož zálohu.",
    )
    val replace = msg("Replace", "Nahradit")
    val lossLine = msg(
        "This deletes {workouts}, {runs}, {routes}, {templates} and {exercises}.",
        "Smaže se: {workouts}, {runs}, {routes}, {templates} a {exercises}.",
    )
}
