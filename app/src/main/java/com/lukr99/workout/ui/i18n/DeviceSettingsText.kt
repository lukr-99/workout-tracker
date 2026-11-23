package com.lukr99.workout.ui.i18n

/** Text for the Android-only Settings rows: Health Connect, automatic backup, the exercise catalog and updates. */
object DeviceSettingsText : TextCatalog("deviceSettings") {
    val healthConnected = msg("Connected", "Připojeno")
    val healthAvailable = msg("Available", "Dostupné")
    val healthUpdateRequired = msg("Provider update required", "Je potřeba aktualizace poskytovatele")
    val healthUnavailable = msg("Unavailable on this device", "Na tomto zařízení nedostupné")
    val healthChecking = msg("Checking availability…", "Zjišťuji dostupnost…")
    val healthPermissionsGranted = msg("Exercise and weight permissions granted", "Oprávnění k cvičení a váze udělena")
    val healthConnectToImport = msg("Connect to import or export workouts", "Připoj se, abys mohl importovat nebo exportovat tréninky")
    val healthInstallOrUpdate = msg("Install or update Health Connect to continue", "Pro pokračování nainstaluj nebo aktualizuj Health Connect")
    val healthNotSupported = msg("Health Connect is not supported by this device", "Toto zařízení Health Connect nepodporuje")
    val healthReadingStatus = msg("Reading provider status", "Zjišťuji stav poskytovatele")
    val healthNotConnected = msg("Not connected", "Nepřipojeno")
    val healthConnect = msg("Connect", "Připojit")
    val healthSendTitle = msg("Send finished workouts and runs", "Posílat dokončené tréninky a běhy")
    val healthSendHint = msg("Each one goes to Health Connect when you finish it.", "Každý se po dokončení odešle do Health Connect.")
    val healthSendAllTitle = msg("Send everything now", "Odeslat vše teď")
    val healthSendAllHint = msg("Workouts and runs Health Connect does not have yet.", "Tréninky a běhy, které Health Connect ještě nemá.")
    val healthExport = msg("Export", "Exportovat")
    val healthBringInTitle = msg("Bring workouts in", "Přenést tréninky sem")
    val healthBringInHint = msg("Imports workouts from other apps as sessions.", "Importuje tréninky z jiných aplikací jako záznamy.")
    val healthImport = msg("Import", "Importovat")
    val healthUpdateNeeded = msg("Update needed", "Chce aktualizaci")
    val healthInstall = msg("Install", "Nainstalovat")
    val privacyTitle = msg("Privacy policy", "Zásady ochrany soukromí")
    val privacyHint = msg("What Ember reads and why.", "Co Ember čte a proč.")
    val healthSummary = msg(
        "Health Connect: imported {imported}, exported {exported}, skipped {skipped}",
        "Health Connect: importováno {imported}, exportováno {exported}, přeskočeno {skipped}",
    )
    val healthCheckFailed = msg("Could not check Health Connect", "Health Connect se nepodařilo zkontrolovat")
    val healthSyncFailed = msg("Health Connect sync failed", "Synchronizace s Health Connect selhala")

    val backupFolderSelected = msg("Backup folder selected", "Složka záloh vybrána")
    val backupNeverRun = msg("No automatic backup has run yet", "Automatická záloha zatím neběžela")
    val backupSucceeded = msg("Succeeded", "Povedlo se")
    val backupFailed = msg("Failed", "Selhalo")
    val backupDisabled = msg("Disabled", "Vypnuto")
    val backupNotRun = msg("Not run", "Neběželo")
    val backupTitle = msg("Automatic backup", "Automatická záloha")
    val backupHint = msg(
        "Saves a full backup to a folder you pick, on this phone or in the cloud.",
        "Uloží plnou zálohu do složky, kterou vybereš, v telefonu nebo v cloudu.",
    )
    val backupHowOften = msg("How often", "Jak často")
    val backupDaily = msg("Daily", "Denně")
    val backupWeekly = msg("Weekly", "Týdně")
    val backupKeepTitle = msg("Backups to keep", "Počet uchovaných záloh")
    val backupKeepHint = msg(
        "Older automatic backups are deleted. Other files are never touched.",
        "Starší automatické zálohy se smažou. Jiné soubory se nikdy nemění.",
    )
    val backupUpdateFailed = msg("Could not update automatic backup", "Automatickou zálohu se nepodařilo upravit")

    val catalogTitle = msg("Exercise catalog", "Katalog cviků")
    val catalogDone = msg("Added {added}, updated {updated}, skipped {skipped}.", "Přidáno {added}, aktualizováno {updated}, přeskočeno {skipped}.")
    val catalogHint = msg(
        "Download the open wger exercise database. Your own exercises are never changed.",
        "Stáhne otevřenou databázi cviků wger. Tvoje vlastní cviky se nikdy nemění.",
    )
    val catalogSync = msg("Sync now", "Synchronizovat")
    val catalogSyncing = msg("Syncing…", "Synchronizuji…")
    val catalogSyncFailed = msg("Sync failed", "Synchronizace selhala")

    val updateVersion = msg("Version {version}", "Verze {version}")
    val updateHint = msg("Checks GitHub for a newer, signed release.", "Zkontroluje na GitHubu novější podepsané vydání.")
    val updateCheckNow = msg("Check now", "Zkontrolovat")
    val updateChecking = msg("Checking…", "Kontroluji…")
    val updateAutoTitle = msg("Check automatically", "Kontrolovat automaticky")
    val updateAutoHint = msg(
        "Looks once a day when you open Ember, and says so on Home when an update is ready.",
        "Podívá se jednou denně při otevření Emberu a na úvodní obrazovce ti řekne, že je aktualizace připravená.",
    )
    val updateReleasesTitle = msg("All releases on GitHub", "Všechna vydání na GitHubu")
    val updateReleasesHint = msg("Release notes and the manual download.", "Poznámky k vydání a ruční stažení.")
    val updateDownload = msg("Download and install", "Stáhnout a nainstalovat")
    val updateLater = msg("Later", "Později")
    val updateOfferTitle = msg("Ember {version}", "Ember {version}", sameInCzech = true)
    val updateNewVersion = msg("A new version is available.", "Je k dispozici nová verze.")
}
