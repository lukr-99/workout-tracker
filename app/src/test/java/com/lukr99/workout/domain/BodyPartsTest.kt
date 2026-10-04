package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyPartsTest {

    @Test
    fun options_listCommonPartsFirstThenCatalogExtrasOnce() {
        val catalog = listOf(
            Exercise(name = "Shrug", primaryBodyPart = "Traps", secondaryBodyParts = listOf("chest")),
            Exercise(name = "Neck curl", primaryBodyPart = "Neck", secondaryBodyParts = listOf("traps")),
        )

        val options = BodyParts.options(catalog)

        assertEquals(BodyParts.common, options.take(BodyParts.common.size))
        assertEquals(listOf("Neck", "Traps"), options.drop(BodyParts.common.size))
    }
}
