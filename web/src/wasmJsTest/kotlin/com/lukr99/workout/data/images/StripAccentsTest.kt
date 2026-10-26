package com.lukr99.workout.data.images

import kotlin.test.Test
import kotlin.test.assertEquals

class StripAccentsTest {
    @Test
    fun removesAccents() {
        assertEquals("Tlak na hrudnik", stripAccents("Tlak na hrudník"))
        assertEquals("Bench Press", stripAccents("Bench Press"))
        assertEquals("Press", stripAccents("Préss"))
    }

    @Test
    fun removesMarksThatAreAlreadyDecomposed() {
        assertEquals("e", stripAccents("é"))
    }

    @Test
    fun leavesPlainTextAndEmptyAlone() {
        assertEquals("", stripAccents(""))
        assertEquals("3/4 sit-up", stripAccents("3/4 sit-up"))
    }
}
