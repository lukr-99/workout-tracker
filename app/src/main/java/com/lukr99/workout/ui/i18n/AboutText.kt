package com.lukr99.workout.ui.i18n

/** Text on the About page. */
object AboutText : TextCatalog("about") {
    val title = msg("About", "O aplikaci")
    val tagline = msg("Strength training and runs, logged fast.", "Silový trénink a běhy, zapsané rychle.")
    val summaryAndroid = msg(
        "A workout log for the gym, with GPS runs, progress charts and records. It works offline and needs no account.",
        "Zápisník tréninků do posilovny, s GPS běhy, grafy pokroku a rekordy. Funguje offline a nepotřebuje účet.",
    )
    val summaryWeb = msg(
        "A workout log for the gym, with progress charts and records. The same app as on Android, here in your browser, with no account.",
        "Zápisník tréninků do posilovny, s grafy pokroku a rekordy. Stejná aplikace jako na Androidu, tady v prohlížeči a bez účtu.",
    )
    val editionAndroid = msg("Android app", "Aplikace pro Android")
    val editionWeb = msg("Web edition", "Webová verze")
    val version = msg("Version", "Verze")
    val versionWith = msg("Version {version}", "Verze {version}")

    val privacyAndroid1 = msg(
        "Your workouts, runs, templates, photos and settings stay on this phone. There is no account and no ads.",
        "Tvoje tréninky, běhy, šablony, fotky a nastavení zůstávají v tomto telefonu. Není tu žádný účet ani reklamy.",
    )
    val privacyAndroid2 = msg(
        "Ember goes online for app updates from GitHub, exercise pictures and catalog sync, map tiles and route snapping.",
        "Ember jde online kvůli aktualizacím z GitHubu, obrázkům cviků a synchronizaci katalogu, mapovým dlaždicím a přichytávání tras.",
    )
    val privacyAndroid3 = msg(
        "Your training data leaves the phone only when you choose: a backup or export, Health Connect, or Claude, if you connect them.",
        "Data o tréninku opustí telefon jen tehdy, když to sám zvolíš: zálohou nebo exportem, přes Health Connect nebo Claude, pokud je připojíš.",
    )
    val privacyAndroid4 = msg(
        "Delete all data in Settings, Your data, removes everything and starts fresh.",
        "Smazání všech dat v Nastavení, Tvoje data, odstraní všechno a začneš znovu.",
    )
    val privacyWeb1 = msg(
        "Your workouts, templates, photos and settings are stored in this browser on this device. There is no account and no ads.",
        "Tvoje tréninky, šablony, fotky a nastavení jsou uložené v tomto prohlížeči na tomto zařízení. Není tu žádný účet ani reklamy.",
    )
    val privacyWeb2 = msg(
        "Nothing is sent anywhere. The page only loads the app's own files.",
        "Nic se nikam neposílá. Stránka jen načítá vlastní soubory aplikace.",
    )
    val privacyWeb3 = msg(
        "Clearing this site's data in the browser deletes everything, so export a backup now and then.",
        "Smazání dat tohoto webu v prohlížeči odstraní všechno, takže občas exportuj zálohu.",
    )

    val creditEmberCredit = msg("Made by {name}. The source is public on GitHub.", "Vytvořil {name}. Zdrojový kód je veřejný na GitHubu.")
    val creditTypefaces = msg("Typefaces", "Písma")
    val creditTypefacesCredit = msg(
        "Barlow and Barlow Condensed by the Barlow Project Authors.",
        "Barlow a Barlow Condensed od autorů projektu Barlow.",
    )
    val creditIcons = msg("Icons", "Ikony")
    val creditIconsCredit = msg("Material Icons by Google.", "Material Icons od Googlu.")
    val creditBuiltWith = msg("Built with", "Postaveno na")
    val creditBuiltWithAndroid = msg(
        "Kotlin, Jetpack Compose, Room, WorkManager, kotlinx libraries, OkHttp, Coil and Firebase Cloud Messaging.",
        "Kotlin, Jetpack Compose, Room, WorkManager, knihovny kotlinx, OkHttp, Coil a Firebase Cloud Messaging.",
    )
    val creditBuiltWithWeb = msg(
        "Kotlin, Compose Multiplatform and kotlinx libraries, compiled to WebAssembly.",
        "Kotlin, Compose Multiplatform a knihovny kotlinx, přeložené do WebAssembly.",
    )
    val creditPictures = msg("Exercise pictures", "Obrázky cviků")
    val creditPicturesCredit = msg(
        "From wger, with the author shown on each picture, and from free-exercise-db.",
        "Z wger, s autorem uvedeným u každého obrázku, a z free-exercise-db.",
    )
    val creditPicturesLicence = msg("CC BY-SA 4.0 and The Unlicense", "CC BY-SA 4.0 a The Unlicense")
    val creditMaps = msg("Maps and routes", "Mapy a trasy")
    val creditMapsCredit = msg(
        "MapLibre draws the map. Tiles from OpenFreeMap, routes snapped by OSRM. Map data by OpenStreetMap contributors.",
        "Mapu kreslí MapLibre. Dlaždice z OpenFreeMap, trasy přichytává OSRM. Mapová data od přispěvatelů OpenStreetMap.",
    )
    val creditMapsLicence = msg("BSD 2-Clause and ODbL", "BSD 2-Clause a ODbL")
    val creditsTitle = msg("Credits and licences", "Poděkování a licence")

    val madeBy = msg("MADE BY", "VYTVOŘIL")
    val authorBlurb = msg(
        "I built Ember for my own training, and I keep making it better.",
        "Ember jsem vytvořil pro vlastní trénink a pořád ho vylepšuji.",
    )
    val feedbackTitle = msg("Tell me what you think", "Napiš mi, co si myslíš")
    val feedbackBody = msg(
        "Feedback is welcome: what works, what does not, what is missing.",
        "Zpětná vazba je vítaná: co funguje, co ne a co chybí.",
    )
    val profileDescription = msg("{site}, {handle}. Opens in the browser.", "{site}, {handle}. Otevře se v prohlížeči.")
    val reportTitle = msg("Report a bug or an idea", "Nahlásit chybu nebo nápad")
    val reportHint = msg("Ember's issues page on GitHub", "Stránka s hlášeními Emberu na GitHubu")

    val factVersion = msg("Version", "Verze")
    val factRunsOn = msg("Runs on", "Běží na")
    val factAccount = msg("Account", "Účet")
    val factNone = msg("None", "Žádný")
    val dataTitle = msg("Your data", "Tvoje data")
    val privacyPolicy = msg("Privacy policy", "Zásady ochrany soukromí")
    val privacyPolicyAndroid = msg("The full text, with Health Connect and Claude.", "Celý text, včetně Health Connect a Clauda.")
    val privacyPolicyWeb = msg("The full text.", "Celý text.")
    val copyright = msg("Copyright 2026 {name}", "Copyright 2026 {name}", sameInCzech = true)
}
