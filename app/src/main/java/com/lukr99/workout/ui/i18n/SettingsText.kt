package com.lukr99.workout.ui.i18n

/** Text on the Settings screen. */
object SettingsText : TextCatalog("settings") {
    val title = msg("Settings", "Nastavení")
    val sectionsLabel = msg("Settings sections", "Sekce nastavení")

    val appearanceTitle = msg("Appearance", "Vzhled")
    val appearanceSummary = msg("How Ember looks on this phone.", "Jak Ember vypadá na tomto telefonu.")
    val workoutsTitle = msg("Workouts", "Tréninky")
    val workoutsSummary = msg("Units, rest and the exercise catalog.", "Jednotky, odpočinek a katalog cviků.")
    val locationsTitle = msg("Locations", "Místa")
    val locationsSummary = msg("Gyms and places you train at.", "Posilovny a místa, kde trénuješ.")
    val healthConnectTitle = msg("Health Connect", "Health Connect", sameInCzech = true)
    val healthConnectSummary = msg("Share workouts and runs with other health apps.", "Sdílej tréninky a běhy s dalšími zdravotními aplikacemi.")
    val claudeTitle = msg("Claude", "Claude", sameInCzech = true)
    val claudeSummary = msg("Let Claude read your training when you allow it, and propose templates.", "Nech Clauda číst tvůj trénink, když to povolíš, a navrhovat šablony.")
    val yourDataTitle = msg("Your data", "Tvoje data")
    val yourDataSummary = msg("Backups, import and export. Everything stays on this phone unless you share it.", "Zálohy, import a export. Vše zůstává v tomto telefonu, dokud to nesdílíš.")
    val updatesTitle = msg("Updates", "Aktualizace")
    val updatesSummary = msg("Ember updates from its GitHub releases.", "Ember se aktualizuje z vydání na GitHubu.")

    val theme = msg("Theme", "Motiv")
    val themeHint = msg("Follows the phone unless you pick one.", "Řídí se telefonem, dokud si nevybereš.")
    val themeSystem = msg("System", "Systém")
    val themeLight = msg("Light", "Světlý")
    val themeDark = msg("Dark", "Tmavý")
    val accent = msg("Accent colour", "Barva zvýraznění")
    val accentHint = msg("The colour of buttons, rings and highlights.", "Barva tlačítek, kruhů a zvýraznění.")
    val language = msg("Language", "Jazyk")
    val languageHint = msg("Follows the phone unless you pick one.", "Řídí se telefonem, dokud si nevybereš.")
    val languageSystem = msg("System", "Systém")
    val languageEnglish = msg("English", "English", sameInCzech = true)
    val languageCzech = msg("Čeština", "Čeština", sameInCzech = true)
    val reduceMotion = msg("Reduce motion", "Omezit pohyb")
    val reduceMotionHint = msg("Jumps and highlights happen at once, with no sliding or fading.", "Skoky a zvýraznění proběhnou hned, bez posouvání a prolínání.")

    val weightUnit = msg("Weight unit", "Jednotka hmotnosti")
    val weightUnitHint = msg("Logged weights are stored in kilograms either way.", "Zapsané váhy se tak či tak ukládají v kilogramech.")
    val kilograms = msg("Kilograms", "Kilogramy")
    val pounds = msg("Pounds", "Libry")
    val defaultRest = msg("Default rest", "Výchozí odpočinek")
    val defaultRestHint = msg("Used when an exercise has no rest of its own.", "Použije se, když cvik nemá vlastní odpočinek.")

    val dataLink = msg("Import, export and delete", "Import, export a smazání")
    val dataLinkHint = msg("Ember backups, Lyfta CSV, plain CSV, and deleting all data.", "Zálohy Emberu, CSV z Lyfty, obyčejné CSV a smazání všech dat.")
    val aboutLink = msg("About Ember", "O aplikaci Ember")
    val aboutLinkHint = msg("Version {version}, who made it, feedback and credits.", "Verze {version}, kdo ji vytvořil, zpětná vazba a poděkování.")

    val accentEmber = msg("Ember", "Ember", sameInCzech = true)
    val accentBlue = msg("Blue", "Modrá")
    val accentGreen = msg("Green", "Zelená")
    val accentRose = msg("Rose", "Růžová")
    val accentPurple = msg("Purple", "Fialová")

    val working = msg("Working…", "Pracuji…")
    val dangerZone = msg("DANGER ZONE", "NEBEZPEČNÁ ZÓNA")

    val locationSaved = msg("Saved the position of {name}.", "Poloha místa {name} je uložená.")
    val locationNoFix = msg(
        "No location fix. Try again outside or near a window.",
        "Poloha se nepodařila zjistit. Zkus to znovu venku nebo u okna.",
    )
    val locationNeedsPrecise = msg(
        "Saving a position needs precise location. Turn on Precise for Ember in the phone's settings.",
        "Uložení polohy potřebuje přesnou polohu. Zapni Přesná poloha pro Ember v nastavení telefonu.",
    )
    val locationNeedsPermission = msg("Location permission is needed to save a position.", "K uložení polohy je potřeba oprávnění k poloze.")
    val addGym = msg("Add a gym", "Přidat posilovnu")
    val addGymHint = msg(
        "Pick the location when you start a workout. Exercises you track per gym keep their numbers apart.",
        "Místo vybereš při zahájení tréninku. Cviky sledované podle posilovny mají čísla odděleně.",
    )
    val add = msg("Add", "Přidat")
    val gymPlaceholder = msg("Brno gym", "Posilovna v Brně")
    val renameLocation = msg("Rename location", "Přejmenovat místo")
    val archivedHint = msg("Archived. Its workouts keep it.", "Archivováno. Tréninky si ho ponechají.")
    val setPosition = msg("Set position", "Nastavit polohu")
    val updatePosition = msg("Update position", "Aktualizovat polohu")
    val restore = msg("Restore", "Obnovit")
    val archive = msg("Archive", "Archivovat")
    val outdoorsKind = msg("Built in. Has no fixed position. Tap to rename.", "Vestavěné. Nemá pevnou polohu. Klepnutím přejmenuješ.")
    val homeKind = msg("Built in. {position}", "Vestavěné. {position}")
    val gymKind = msg("Gym. {position}", "Posilovna. {position}")
    val noPosition = msg("No position yet, so it is not suggested by GPS.", "Zatím bez polohy, takže ho GPS nenavrhne.")
    val positionSaved = msg("Position saved. Suggested when you are near.", "Poloha uložená. Navrhne se, když jsi poblíž.")
}
