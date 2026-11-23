package com.lukr99.workout.ui.i18n

/** Text of the privacy policy and the Health Connect permission rationale. */
object PrivacyPolicyText : TextCatalog("privacyPolicy") {
    val rationaleTitle = msg("Health permissions", "Zdravotní oprávnění")
    val rationaleSubtitle = msg("Why Ember requests access", "Proč Ember žádá o přístup")
    val title = msg("Privacy policy", "Zásady ochrany soukromí")
    val subtitle = msg("Your data stays under your control", "Tvoje data zůstávají pod tvou kontrolou")

    val healthDataTitle = msg("Health data we use", "Zdravotní data, která používáme")
    val healthDataBody = msg(
        "With your permission, Ember reads exercise sessions and weight records from Health Connect. " +
            "It can write completed workout sessions and the bodyweight recorded with those sessions.",
        "S tvým svolením Ember čte z Health Connect záznamy o cvičení a hmotnosti. " +
            "Může zapisovat dokončené tréninky a tělesnou hmotnost zaznamenanou s nimi.",
    )

    val accessTitle = msg("Why access is needed", "Proč je přístup potřeba")
    val accessBody = msg(
        "Read access powers the Import action so workouts from other apps can appear in your history. " +
            "Write access powers the Export action so workouts completed here can be shared through " +
            "Health Connect. Sync only runs when you choose Import or Export.",
        "Čtení umožňuje akci Import, aby se tréninky z jiných aplikací mohly objevit v tvé historii. " +
            "Zápis umožňuje akci Export, aby se tréninky dokončené tady daly sdílet přes Health Connect. " +
            "Synchronizace proběhne jen tehdy, když zvolíš Import nebo Export.",
    )

    val storageTitle = msg("Storage and sharing", "Uložení a sdílení")
    val storageBody = msg(
        "Workout data is stored in the app on this device. Health data is exchanged directly with " +
            "Health Connect; Ember does not sell health data. Automatic backups are off by default and " +
            "only write to a folder you select.",
        "Data o trénincích jsou uložená v aplikaci na tomto zařízení. Zdravotní data se předávají " +
            "přímo s Health Connect; Ember zdravotní data neprodává. Automatické zálohy jsou ve " +
            "výchozím stavu vypnuté a zapisují jen do složky, kterou vybereš.",
    )

    val claudeTitle = msg("Claude", "Claude", sameInCzech = true)
    val claudeBody = msg(
        "Claude is off until you connect it in Settings, Claude, to a server you run yourself. " +
            "Then, when Claude asks and you allow it (or always, if you choose), Ember sends your " +
            "workouts, runs without their GPS track, exercises and templates to that server. " +
            "Workouts you imported from Health Connect are among them. The copy is deleted after " +
            "30 minutes. Photos and settings are never sent. Claude can only propose templates " +
            "and exercises, which change nothing until you accept them.",
        "Claude je vypnutý, dokud ho v Nastavení, Claude nepropojíš se serverem, který si sám provozuješ. " +
            "Potom, když se Claude zeptá a ty to povolíš (nebo vždy, pokud si to zvolíš), Ember na ten " +
            "server pošle tvoje tréninky, běhy bez GPS stopy, cviky a šablony. Jsou mezi nimi i tréninky " +
            "importované z Health Connect. Kopie se po 30 minutách smaže. Fotky a nastavení se nikdy " +
            "neposílají. Claude může jen navrhovat šablony a cviky, které nic nezmění, dokud je nepřijmeš.",
    )

    val choicesTitle = msg("Your choices", "Tvoje možnosti")
    val choicesBody = msg(
        "Health Connect access is optional. You can review or revoke permissions in Health Connect " +
            "at any time, keep using Ember without connecting it, and disable automatic backup from " +
            "Settings. Claude can be set to never share, and removed from Settings, Claude at any time.",
        "Přístup k Health Connect je dobrovolný. Oprávnění můžeš kdykoli zkontrolovat nebo odvolat " +
            "v Health Connect, Ember můžeš dál používat bez propojení a automatickou zálohu můžeš " +
            "vypnout v Nastavení. Claude můžeš nastavit, aby nikdy nesdílel, a kdykoli ho odebrat " +
            "v Nastavení, Claude.",
    )

    val lastUpdated = msg("Last updated: November 11, 2026", "Naposledy aktualizováno: 11. listopadu 2026")
}
