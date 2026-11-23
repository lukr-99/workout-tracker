package com.lukr99.workout.ui.i18n

import com.lukr99.workout.domain.ExerciseCategory

/** Text of the exercise picker and the quick create sheet. */
object PickerText : TextCatalog("picker") {
    val addExercise = msg("Add exercise", "Přidat cvik")
    val illustration = msg("{name} illustration", "Ilustrace: {name}")
    val searchOrNew = msg("Search or type a new name", "Hledej nebo napiš nový název")
    val searchExercises = msg("Search exercises", "Hledat cviky")
    val all = msg("All", "Vše")
    val recent = msg("Recent", "Nedávné")
    val allExercises = msg("All exercises", "Všechny cviky")
    val noMatch = msg("No exercises match these filters.", "Žádný cvik neodpovídá filtrům.")
    val noneCalled = msg("No exercise called “{name}” yet.", "Cvik „{name}“ zatím neexistuje.")
    val create = msg("Create “{name}”", "Vytvořit „{name}“")
    val quickForm = msg("A quick form. You stay in the workout.", "Rychlý formulář. Zůstaneš v tréninku.")
    val asSuperset = msg("As a superset", "Jako superserie")
    val pickExercises = msg("Pick exercises", "Vyber cviky")
    val addCount = msg("Add {n}", "Přidat {n}")

    val newExercise = msg("New exercise", "Nový cvik")
    val savedToLibrary = msg(
        "Saved to your library and added to this workout.",
        "Uloží se do knihovny a přidá do tréninku.",
    )
    val name = msg("Name", "Název")
    val kind = msg("Kind", "Druh")
    val mainMuscle = msg("Main muscle", "Hlavní sval")
    val alsoWorks = msg("Also works", "Zapojuje také")
    val equipmentLabel = msg("Equipment", "Vybavení")
    val canWait = msg(
        "Steps, a photo and a guide link can wait. Add them later from the Library.",
        "Kroky, fotka a odkaz na návod počkají. Přidej je později v Knihovně.",
    )
    val createAndAdd = msg("Create and add", "Vytvořit a přidat")

    private val categoryStrength = msg("Strength", "Síla")
    private val categoryCardio = msg("Cardio", "Kardio")

    /** The display name of an exercise kind. */
    fun category(category: ExerciseCategory): Msg =
        if (category == ExerciseCategory.Strength) categoryStrength else categoryCardio

    private val equipmentBarbell = msg("Barbell", "Velká činka")
    private val equipmentDumbbell = msg("Dumbbell", "Jednoručka")
    private val equipmentCable = msg("Cable", "Kladka")
    private val equipmentMachine = msg("Machine", "Stroj")
    private val equipmentBodyWeight = msg("Body weight", "Tělesná hmotnost")
    private val equipmentKettlebell = msg("Kettlebell", "Kettlebell", sameInCzech = true)
    private val equipmentBand = msg("Band", "Guma")
    private val equipmentOther = msg("Other", "Jiné")

    /** The display name of an equipment value. The stored value stays the English key. */
    fun equipment(key: String): Msg = when (key) {
        "Barbell" -> equipmentBarbell
        "Dumbbell" -> equipmentDumbbell
        "Cable" -> equipmentCable
        "Machine" -> equipmentMachine
        "Body weight" -> equipmentBodyWeight
        "Kettlebell" -> equipmentKettlebell
        "Band" -> equipmentBand
        else -> equipmentOther
    }
}
