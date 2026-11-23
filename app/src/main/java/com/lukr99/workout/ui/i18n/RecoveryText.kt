package com.lukr99.workout.ui.i18n

import com.lukr99.workout.domain.recovery.MuscleGroup

/** Text of the muscle map card and its info sheet. */
object RecoveryText : TextCatalog("recovery") {
    // Card
    val modeRecovery = msg("Recovery", "Regenerace")
    val modeWeeklySets = msg("Weekly sets", "Týdenní série")
    val muscleRecovery = msg("Muscle recovery", "Regenerace svalů")
    val howCalculated = msg("How this is calculated", "Jak se to počítá")
    val percentReady = msg("{pct}% ready", "{pct} % připraveno")
    val inRangeCount = msg("{n} of {total} in range", "{n} z {total} v rozsahu")
    val below = msg("Below {v}", "Pod {v}")
    val rangeSets = msg("{from} to {to} sets", "{from} až {to} sérií")
    val above = msg("Above {v}", "Nad {v}")
    val noSets = msg("No sets in the last two weeks.", "Žádné série za poslední dva týdny.")
    val stateReady = msg("Ready", "Připraveno")
    val stateRecovering = msg("Recovering", "Regeneruje se")
    val stateFatigued = msg("Fatigued", "Unavené")
    val weeklyNone = msg("None", "Žádné")
    val weeklyBelow = msg("Below", "Pod")
    val weeklyInRange = msg("In range", "V rozsahu")
    val weeklyAbove = msg("Above", "Nad")
    val readyInHours = msg("Ready in {n} h", "Za {n} h")
    val readyInDays = msg("Ready in {n} d", "Za {n} d")

    /** A set count that may be a half step, like 7.5. [v] is the shown number, `n` picks the form. */
    val setsCount = plural("{v} set", "{v} sets", "{v} série", "{v} série", "{v} sérií")

    // Info sheet
    val infoTitle = msg("How the muscle map works", "Jak funguje mapa svalů")
    val headingRecovery = msg("Recovery", "Regenerace")
    val headingWeekly = msg("Weekly sets", "Týdenní série")
    val headingKeepInMind = msg("Keep in mind", "Pamatuj")
    val headingSources = msg("Sources", "Zdroje")
    val openSource = msg("Open source", "Otevřít zdroj")

    val infoFatigueHalf = msg(
        "Each working set adds fatigue to the muscles it trains. The main muscle gets a full set " +
            "and each secondary muscle gets half a set. Warm-ups do not count.",
        "Každá pracovní série přidává únavu svalům, které trénuje. Hlavní sval dostane celou sérii " +
            "a každý vedlejší sval polovinu série. Zahřívací série se nepočítají.",
    )
    val infoFatiguePercent = msg(
        "Each working set adds fatigue to the muscles it trains. The main muscle gets a full set " +
            "and each secondary muscle gets {p}% of a set. Warm-ups do not count.",
        "Každá pracovní série přidává únavu svalům, které trénuje. Hlavní sval dostane celou sérii " +
            "a každý vedlejší sval {p} % série. Zahřívací série se nepočítají.",
    )
    val infoEffort = msg(
        "Sets close to failure count more. A set to failure counts as {max} sets and an " +
            "easy set as {min}. Without reps in reserve or RPE, a set counts as 1. " +
            "Negatives and slow lowering count a bit more, because they cause more muscle damage.",
        "Série blízko selhání se počítají víc. Série do selhání se počítá jako {max} série a lehká " +
            "série jako {min}. Bez rezervy v opakováních nebo RPE se série počítá jako 1. " +
            "Negativní série a pomalé spouštění se počítají o něco víc, protože víc poškozují sval.",
    )
    val infoFades = msg(
        "Fatigue fades over time. Half of it is gone after about {times}. " +
            "Big leg muscles take longest. A muscle shows as ready when 6 hard sets would be about " +
            "90% recovered, which is 1.5 to 3 days for most muscles.",
        "Únava časem mizí. Polovina je pryč zhruba za {times}. " +
            "Velké svaly nohou trvají nejdéle. Sval se ukáže jako připravený, když by se po 6 těžkých " +
            "sériích zotavil asi z 90 %, což je u většiny svalů 1,5 až 3 dny.",
    )
    val swatchReady = msg("Ready: {pct}% or more", "Připraveno: {pct} % a více")
    val swatchRecovering = msg("Recovering: {from} to {to}%", "Regeneruje se: {from} až {to} %")
    val swatchFatigued = msg("Fatigued: below {pct}%", "Unavené: pod {pct} %")
    val swatchGray = msg("Gray: not trained in the last 2 weeks", "Šedá: bez tréninku za poslední 2 týdny")

    val infoWeeklyCount = msg(
        "Counts hard sets in the last 7 days, with the same full and half sets. A set with " +
            "{n} or more reps in reserve is too easy to count.",
        "Počítá těžké série za posledních 7 dní, stejně s celými a polovičními sériemi. " +
            "Série s rezervou {n} a více opakování je příliš lehká a nepočítá se.",
    )
    val infoStudies = msg(
        "Studies link {from} to {to} hard sets per muscle per week " +
            "with the most muscle growth. Fewer still help. More add little and are harder to recover from. " +
            "The line under each bar marks that range.",
        "Studie spojují {from} až {to} těžkých sérií na sval týdně s největším růstem svalů. " +
            "I méně pomáhá. Více přidá málo a hůř se z toho regeneruje. " +
            "Čára pod každým sloupcem značí tento rozsah.",
    )
    val swatchBelowSets = msg("Below {v} sets", "Pod {v} sérií")
    val swatchRangeSets = msg("{from} to {to} sets: in range", "{from} až {to} sérií: v rozsahu")
    val swatchAboveSets = msg("Above {v} sets", "Nad {v} sérií")
    val infoEstimate = msg(
        "This is an estimate. Sleep, food, stress and how used you are to the training matter too. " +
            "If a muscle still feels sore or weak, give it more time.",
        "Je to odhad. Záleží i na spánku, jídle, stresu a na tom, jak je tělo na trénink zvyklé. " +
            "Když sval pořád bolí nebo je slabý, dej mu víc času.",
    )

    // The sentence about half-lives, like "12 hours for forearms and abs, 15 for shoulders and 20 for chest"
    val and = msg("and", "a")
    val firstHalfLife = msg("{h} hours for {list}", "{h} hodin pro {list}")
    val nextHalfLife = msg("{h} for {list}", "{h} pro {list}")

    private val chest = msg("chest", "prsa")
    private val back = msg("back", "záda")
    private val shoulders = msg("shoulders", "ramena")
    private val biceps = msg("biceps", "bicepsy")
    private val triceps = msg("triceps", "tricepsy")
    private val forearms = msg("forearms", "předloktí")
    private val abs = msg("abs", "břicho")
    private val glutes = msg("glutes", "hýždě")
    private val quads = msg("quads", "přední stehna")
    private val hamstrings = msg("hamstrings", "zadní stehna")
    private val calves = msg("calves", "lýtka")

    /** The lower-case name of a muscle inside a sentence. */
    fun muscleName(group: MuscleGroup): Msg = when (group) {
        MuscleGroup.Chest -> chest
        MuscleGroup.Back -> back
        MuscleGroup.Shoulders -> shoulders
        MuscleGroup.Biceps -> biceps
        MuscleGroup.Triceps -> triceps
        MuscleGroup.Forearms -> forearms
        MuscleGroup.Abs -> abs
        MuscleGroup.Glutes -> glutes
        MuscleGroup.Quads -> quads
        MuscleGroup.Hamstrings -> hamstrings
        MuscleGroup.Calves -> calves
    }
}
