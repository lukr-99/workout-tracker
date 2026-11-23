package com.lukr99.workout.ui.i18n

/** Text on the Library screen (templates and exercise catalog) and the exercise editor. */
object LibraryText : TextCatalog("library") {
    val title = msg("Library", "Knihovna")
    val exercisesCount = plural("{n} exercise", "{n} exercises", "{n} cvik", "{n} cviky", "{n} cviků")
    val templatesCount = plural("{n} template", "{n} templates", "{n} šablona", "{n} šablony", "{n} šablon")
    val newTemplate = msg("New template", "Nová šablona")
    val newExercise = msg("New exercise", "Nový cvik")
    val tabExercises = msg("Exercises", "Cviky")
    val tabTemplates = msg("Templates", "Šablony")
    val noTemplates = msg("No templates yet", "Zatím žádné šablony")
    val noTemplatesBody = msg(
        "A template is a saved list of exercises to start a workout from, with a plan for each one if you like.",
        "Šablona je uložený seznam cviků, ze kterého začneš trénink. Ke každému cviku můžeš přidat plán.",
    )
    val previewName = msg("Preview {name}", "Náhled: {name}")
    val startName = msg("Start {name}", "Začít: {name}")
    val noExercises = msg("No exercises", "Žádné cviky")

    val searchPlaceholder = msg("Search exercises", "Hledat cviky")
    val chipAll = msg("All", "Vše")
    val chipStrength = msg("Strength", "Silové")
    val chipCardio = msg("Cardio", "Kardio")
    val chipArchived = msg("Archived", "Archivované")
    val chipAnyEquipment = msg("Any equipment", "Jakékoli vybavení")
    val noMatch = msg("No exercises match", "Žádný cvik neodpovídá")
    val noMatchBody = msg(
        "Clear a filter or try another word. Or add it as your own exercise.",
        "Zruš filtr nebo zkus jiné slovo. Nebo ho přidej jako vlastní cvik.",
    )
    val createExercise = msg("Create exercise", "Vytvořit cvik")
    val createNamed = msg("Create “{name}”", "Vytvořit „{name}“")
    val editName = msg("Edit {name}", "Upravit {name}")
    val exerciseNote = msg("Exercise note", "Poznámka ke cviku")
    val restoreName = msg("Restore {name}", "Obnovit {name}")
    val archiveName = msg("Archive {name}", "Archivovat {name}")

    // Exercise editor
    val editTitle = msg("Edit exercise", "Upravit cvik")
    val loading = msg("Loading…", "Načítám…")
    val sectionImage = msg("Exercise image", "Obrázek cviku")
    val imagePersonal = msg("Personal photo", "Vlastní fotka")
    val imageExercise = msg("Exercise image", "Obrázek cviku")
    val imagePlaceholder = msg("Body-part placeholder", "Zástupný obrázek části těla")
    val remove = msg("Remove", "Odebrat")
    val takePhoto = msg("Take photo", "Vyfotit")
    val choosePhoto = msg("Choose", "Vybrat")
    val photoSaveFailed = msg("Could not save the photo", "Fotku se nepodařilo uložit")
    val name = msg("Name", "Název")
    val categoryStrength = msg("Strength", "Silový")
    val categoryCardio = msg("Cardio", "Kardio")
    val sectionMainMuscle = msg("Main muscle", "Hlavní sval")
    val orTypeOne = msg("Or type one", "Nebo napiš vlastní")
    val sectionAlsoWorks = msg("Also works", "Zapojuje také")
    val equipment = msg("Equipment (optional)", "Vybavení (nepovinné)")
    val defaultRest = msg("Default rest (seconds, optional)", "Výchozí odpočinek (sekundy, nepovinné)")
    val sectionHowTo = msg("How to do it", "Jak ho provést")
    val steps = msg("Steps (optional)", "Postup (nepovinné)")
    val stepsHint = msg(
        "One step per line. They show as a numbered list behind the info button while you log.",
        "Jeden krok na řádek. Při zapisování se ukážou jako číslovaný seznam za tlačítkem info.",
    )
    val guideLink = msg("Guide or video link (optional)", "Odkaz na návod nebo video (nepovinné)")
    val linkInvalid = msg("This does not look like a web address.", "Tohle nevypadá jako webová adresa.")
    val linkExample = msg("For example a YouTube video. Opens in your browser.", "Třeba video na YouTube. Otevře se v prohlížeči.")
    val sectionNote = msg("Your note", "Tvoje poznámka")
    val noteLabel = msg("Note (optional)", "Poznámka (nepovinné)")
    val notePlaceholder = msg("Seat on 4, grip just outside the rings", "Sedadlo na 4, úchop těsně vně kruhů")
    val noteHint = msg("Pinned to this exercise every time you log it.", "Připne se ke cviku pokaždé, když ho zapisuješ.")
    val sectionGyms = msg("Gyms", "Posilovny")
    val created = msg("Exercise created", "Cvik vytvořen")
    val saved = msg("Exercise saved", "Cvik uložen")
    val couldNotSave = msg("Could not save", "Uložení se nepovedlo")
    val createButton = msg("Create exercise", "Vytvořit cvik")
    val saveChanges = msg("Save changes", "Uložit změny")
}
