package com.lukr99.workout.ui.i18n

/** Text of the location chip, picker, setup card, position offer and the per-gym prompts. */
object LocationText : TextCatalog("location") {
    val changeLocation = msg("Change location", "Změnit místo")
    val chip = msg("Location: {name}", "Místo: {name}")
    val pickerHelp = msg(
        "Exercises you track per gym show the numbers from here.",
        "Cviky sledované podle posilovny ukážou čísla odsud.",
    )
    val unknownLocation = msg("Unknown location", "Neznámé místo")
    val addGym = msg("Add a gym", "Přidat posilovnu")
    val gymPlaceholder = msg("Brno gym", "Posilovna v Brně")
    val add = msg("Add", "Přidat")
    val selected = msg("Selected", "Vybráno")

    val whereTrain = msg("Where do you train?", "Kde trénuješ?")
    val nameMainGym = msg(
        "Name your main gym. Machines differ between gyms, so Ember can keep their numbers apart.",
        "Pojmenuj svou hlavní posilovnu. Stroje se v posilovnách liší, takže Ember může držet jejich čísla zvlášť.",
    )
    val pastWorkoutsThere = plural(
        "My past workout was there",
        "My {n} past workouts were there",
        "Můj dřívější trénink byl tam",
        "Mé {n} dřívější tréninky byly tam",
        "Mých {n} dřívějších tréninků bylo tam",
    )
    val otherwiseUnknown = msg(
        "Otherwise they stay at Unknown location and still count everywhere. You can move them later in History.",
        "Jinak zůstanou na neznámém místě a počítají se všude. Později je můžeš přesunout v Historii.",
    )
    val notNow = msg("Not now", "Teď ne")

    val saveSpot = msg("Save this spot as {name}?", "Uložit toto místo jako {name}?")
    val no = msg("No", "Ne")

    val splitTitle = msg("Track {name} separately per gym?", "Sledovat {name} zvlášť podle posilovny?")
    val splitBody = msg(
        "{detail}. Per gym, last time, records and progress only count workouts at this gym.",
        "{detail}. Podle posilovny se minule, rekordy a pokrok počítají jen z tréninků v této posilovně.",
    )
    val dontAskAgain = msg("Don't ask again", "Už se neptat")
    val trackPerGym = msg("Track per gym", "Sledovat podle posilovny")
    val trackPerGymHelp = msg(
        "For machines that differ between gyms. Last time, records and progress only count workouts " +
            "at the gym you are in. Turn it off to see all gyms together again; no history is lost.",
        "Pro stroje, které se v posilovnách liší. Minule, rekordy a pokrok se počítají jen z tréninků " +
            "v posilovně, kde právě jsi. Po vypnutí uvidíš zase všechny posilovny dohromady; žádná historie se neztratí.",
    )
}
