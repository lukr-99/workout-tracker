package com.lukr99.workout.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tag ordinals are stored in Room and in backups, so the order may only grow at the end. */
class SetTagTest {

    @Test
    fun ordinalsStayWhereTheyWereStored() {
        assertEquals(
            listOf(
                "Warmup", "Drop", "ToFailure", "Failed", "Negative", "BackOff",
                "Finisher", "Explosive", "SlowEccentric", "SlowConcentric",
            ),
            SetTag.entries.map { it.name },
        )
    }

    @Test
    fun newTagsCombineAndLeaveTheOldSetTypeNormal() {
        val set = StrengthSet(id = "s1", setNumber = 1, reps = 8, weightKg = 60.0)
            .withTagToggled(SetTag.Finisher)
            .withTagToggled(SetTag.Explosive)
            .withTagToggled(SetTag.SlowEccentric)
            .withTagToggled(SetTag.SlowConcentric)

        assertEquals(
            setOf(SetTag.Finisher, SetTag.Explosive, SetTag.SlowEccentric, SetTag.SlowConcentric),
            set.tags,
        )
        assertEquals(SetType.Normal, set.setType)
        assertEquals(false, set.isWarmup)

        val withDrop = set.withTagToggled(SetTag.Drop)
        assertEquals(SetType.Drop, withDrop.setType)
    }
}
