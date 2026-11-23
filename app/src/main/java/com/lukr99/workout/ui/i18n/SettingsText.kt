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
}
