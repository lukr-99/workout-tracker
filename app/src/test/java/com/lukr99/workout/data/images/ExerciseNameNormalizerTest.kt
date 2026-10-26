package com.lukr99.workout.data.images

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseNameNormalizerTest {
    @Test
    fun collapsesPunctuationWhitespaceAndEquipmentSuffixes() {
        assertEquals(
            "hammer strength iso chest press mygym",
            ExerciseNameNormalizer.normalize(
                "  Hammer Strength Iso Chest Press — MyGym (Machine) ",
            ),
        )
    }

    @Test
    fun stripsAccentsBeforeMatching() {
        assertEquals("drep s cinkou", ExerciseNameNormalizer.normalize("Dřep s činkou"))
        assertEquals("press", ExerciseNameNormalizer.normalize("Préss"))
    }

    @Test
    fun appliesKnownCatalogAliases() {
        assertEquals(
            ExerciseNameNormalizer.normalize("Barbell Full Squat"),
            ExerciseNameNormalizer.normalize("Back Squat"),
        )
        assertEquals(
            ExerciseNameNormalizer.normalize("Running, Treadmill"),
            ExerciseNameNormalizer.normalize("Treadmill Run"),
        )
    }
}
