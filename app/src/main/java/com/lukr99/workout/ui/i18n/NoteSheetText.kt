package com.lukr99.workout.ui.i18n

/** Text of the note sheet opened from a workout, an exercise or a set. */
object NoteSheetText : TextCatalog("noteSheet") {
    val workoutTitle = msg("Workout note", "Poznámka k tréninku")
    val workoutHint = msg(
        "How the session felt, sleep, anything worth remembering. Shown in your history.",
        "Jak trénink šel, spánek, cokoli, co stojí za zapamatování. Zobrazí se v historii.",
    )
    val entryTitle = msg("Note · {name}", "Poznámka · {name}")
    val entryHint = msg(
        "Next time you log this exercise, this note shows up as \"Last time\".",
        "Až tento cvik příště zapíšeš, poznámka se ukáže jako „Minule“.",
    )
    val entryPlaceholder = msg("Grip felt off, go up 2.5 kg next time", "Úchop nesedl, příště přidej 2,5 kg")
    val setTitle = msg("Set {n} note", "Poznámka k sérii {n}")
    val setHint = msg(
        "A note on just this set, like a spotter or a pause.",
        "Poznámka jen k této sérii, třeba o dopomoci nebo pauze.",
    )
}
