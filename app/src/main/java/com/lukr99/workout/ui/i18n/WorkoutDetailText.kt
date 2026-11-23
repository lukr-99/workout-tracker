package com.lukr99.workout.ui.i18n

/** Text of the past-workout screen, the share preview and the share and file error messages. */
object WorkoutDetailText : TextCatalog("workoutDetail") {
    val loading = msg("Loading…", "Načítám…")
    val preparingShare = msg("Preparing share preview", "Připravuji náhled sdílení")
    val shareWorkout = msg("Share workout", "Sdílet trénink")
    val shareImage = msg("Share image", "Sdílet obrázek")
    val shareAsText = msg("Share as text", "Sdílet jako text")
    val couldNotPrepareImage = msg("Could not prepare the share image", "Obrázek ke sdílení se nepodařilo připravit")
    val couldNotOpenShareSheet = msg("Could not open the share sheet", "Nabídku sdílení se nepodařilo otevřít")
    val workoutFallback = msg("Workout", "Trénink")
    val addNote = msg("Add note", "Přidat poznámku")
    val editNote = msg("Edit note", "Upravit poznámku")
    val removeExercise = msg("Remove exercise", "Odebrat cvik")
    val addSet = msg("Add set", "Přidat sérii")
    val addExercise = msg("Add exercise", "Přidat cvik")
    val makeTemplate = msg("Make template", "Vytvořit šablonu")
    val whereWasWorkout = msg("Where was this workout?", "Kde proběhl tento trénink?")
    val deleteTitle = msg("Delete workout?", "Smazat trénink?")
    val deleteMessage = msg(
        "This permanently removes the session from your history.",
        "Trénink se trvale odstraní z tvé historie.",
    )

    // Share preview sheet
    val sharePreview = msg("Share preview", "Náhled sdílení")
    val sharePreviewHint = msg("This exact image will be shared.", "Sdílet se bude přesně tento obrázek.")
    val sharePreviewDescription = msg("Workout share image preview", "Náhled obrázku tréninku ke sdílení")
    val share = msg("Share", "Sdílet")

    // Platform share and file actions
    val shareChooserTitle = msg("Share workout", "Sdílet trénink")
    val shareSheetFailed = msg("Could not open the share sheet", "Nabídku sdílení se nepodařilo otevřít")
    val photoNotOpened = msg("The selected image could not be opened.", "Vybraný obrázek se nepodařilo otevřít.")
    val photoNotCopied = msg("Could not copy the selected photo", "Vybranou fotku se nepodařilo zkopírovat")
    val cameraNoImage = msg("The camera did not return an image.", "Fotoaparát nevrátil žádný obrázek.")
    val cameraNotSaved = msg("Could not save the camera photo", "Fotku z fotoaparátu se nepodařilo uložit")
    val cameraNotOpened = msg("Could not open the camera", "Fotoaparát se nepodařilo otevřít")
    val cameraTargetFailed = msg("Could not create a camera image target.", "Nepodařilo se vytvořit soubor pro fotku z fotoaparátu.")
}
