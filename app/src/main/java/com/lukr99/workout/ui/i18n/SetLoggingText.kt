package com.lukr99.workout.ui.i18n

/** Text of the set row, set entry sheet, set options sheet and the rest timer bar. */
object SetLoggingText : TextCatalog("setlogging") {
    // Rest timer
    val rest = msg("Rest", "Odpočinek")
    val restNext = msg("Rest · next is {next}", "Odpočinek · další je {next}")
    val skip = msg("Skip", "Přeskočit")

    // Set entry sheet
    val weight = msg("Weight", "Váha")
    val reps = msg("Reps", "Opakování")
    val nextReps = msg("Next: reps", "Dál: opakování")
    val backToWeight = msg("Back to weight", "Zpět na váhu")

    // Set options sheet
    val setOptions = msg("Set options", "Možnosti série")
    val tagsHint = msg("Tags · choose any that apply", "Štítky · vyber, co platí")
    val repsLeft = msg("Reps left in the tank", "Rezerva v opakováních")
    val reserveEmpty = msg(
        "How many more reps you could have done. Leave it empty if you did not track it.",
        "Kolik opakování bys ještě zvládl. Nech prázdné, pokud jsi to nesledoval.",
    )
    val reserveOrEasier = msg(
        "Same as RPE {rpe} or easier. Tap it again to clear.",
        "Odpovídá RPE {rpe} nebo lehčímu. Klepnutím znovu to smažeš.",
    )
    val reserveRpe = msg("Same as RPE {rpe}. Tap it again to clear.", "Odpovídá RPE {rpe}. Klepnutím znovu to smažeš.")
    val note = msg("Note", "Poznámka")
    val addNoteToSet = msg("Add a note to this set", "Přidej poznámku k této sérii")
    val addSetNote = msg("Add set note", "Přidat poznámku k sérii")
    val editSetNote = msg("Edit set note", "Upravit poznámku k sérii")
    val duplicate = msg("Duplicate", "Duplikovat")
    val removeSet = msg("Remove set", "Odebrat sérii")

    // Set tags
    val tagWarmup = msg("Warm-up", "Zahřívací")
    val tagDrop = msg("Drop", "Drop set")
    val tagToFailure = msg("To failure", "Do selhání")
    val tagFailed = msg("Failed early", "Předčasné selhání")
    val tagNegative = msg("Negative", "Negativní")
    val tagBackOff = msg("Back-off", "Ústupová")
    val tagFinisher = msg("Finisher", "Dokončovací")
    val tagExplosive = msg("Explosive", "Výbušná")
    val tagSlowDown = msg("Slow down", "Pomalu dolů")
    val tagSlowUp = msg("Slow up", "Pomalu nahoru")

    // Set row
    val lastTimeFor = plural(
        "Last time {w} {unit} for {n} rep",
        "Last time {w} {unit} for {n} reps",
        "Minule {w} {unit} na {n} opakování",
        "Minule {w} {unit} na {n} opakování",
        "Minule {w} {unit} na {n} opakování",
    )
    val noLastTime = msg("No last time", "Minule nic")
    val weightSet = msg("Weight, set {n}", "Váha, série {n}")
    val repsSet = msg("Reps, set {n}", "Opakování, série {n}")
    val lastTimeInline = msg(" · last time {w} × {reps}", " · minule {w} × {reps}")
    val setOfTotal = msg("Set {n} of {total}", "Série {n} z {total}")
    val setNumber = msg("Set {n}", "Série {n}")
    val setDone = msg("Set {n} done", "Série {n} hotová")
    val setNote = msg("Set note", "Poznámka k sérii")

    // Short badges on the set number circle and on the chips
    val badgePr = msg("PR", "OR")
    val badgeWarmup = msg("W", "Z")
    val badgeToFailure = msg("TF", "DS")
    val badgeDrop = msg("D", "D", sameInCzech = true)
    val badgeFinisher = msg("F", "DK")

    // Column header above the set list
    val columnSet = msg("SET", "SÉR.")
    val columnPrevious = msg("PREVIOUS", "MINULE")
    val columnReps = msg("REPS", "OPAK.")
}
