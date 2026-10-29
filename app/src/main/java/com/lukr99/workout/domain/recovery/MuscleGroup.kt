package com.lukr99.workout.domain.recovery

/**
 * The muscles the recovery model and the body map know. Each one has its own recovery speed in
 * [RecoveryConfig.halfLifeHours]. [aliases] are the lower-case body part names that mean this muscle,
 * from the exercise editor, the seed catalog, wger and free exercise data.
 */
enum class MuscleGroup(val label: String, val aliases: Set<String>) {
    Chest("Chest", setOf("chest", "pecs", "pectorals", "pectoralis major", "pectoralis minor", "serratus anterior")),
    Back(
        "Back",
        setOf(
            "back", "lats", "latissimus dorsi", "traps", "trapezius", "upper back", "middle back",
            "lower back", "rhomboids", "erector spinae",
        ),
    ),
    Shoulders(
        "Shoulders",
        setOf(
            "shoulders", "delts", "deltoids", "anterior deltoid", "lateral deltoid", "posterior deltoid",
            "front delts", "side delts", "rear delts",
        ),
    ),
    Biceps("Biceps", setOf("biceps", "biceps brachii", "brachialis")),
    Triceps("Triceps", setOf("triceps", "triceps brachii")),
    Forearms("Forearms", setOf("forearms", "forearm", "brachioradialis", "grip")),
    Abs(
        "Abs",
        setOf(
            "abs", "core", "abdominals", "rectus abdominis", "obliques", "obliquus externus abdominis",
            "transverse abdominis",
        ),
    ),
    Glutes("Glutes", setOf("glutes", "gluteus maximus", "gluteus medius", "hips", "abductors", "adductors")),
    Quads("Quads", setOf("quads", "quadriceps", "quadriceps femoris")),
    Hamstrings("Hamstrings", setOf("hamstrings", "biceps femoris")),
    Calves("Calves", setOf("calves", "calf", "gastrocnemius", "soleus")),
}
