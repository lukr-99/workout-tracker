package com.lukr99.workout.data.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class JsonNumberRepairTest {

    @Test
    fun putsTheLostDigitBackInNumbersOnly() {
        assertEquals(
            """{"a":1e-7,"b":[1e-7,-1e-7],"c": 1e-7 ,"note":"x:.e-7,y \".e-7"}""",
            JsonNumberRepair.repair("""{"a":.e-7,"b":[.e-7,-.e-7],"c": .e-7 ,"note":"x:.e-7,y \".e-7"}"""),
        )
    }

    @Test
    fun leavesGoodJsonAlone() {
        val json = """{"a":2.5e-7,"b":"1.e5 and .e-7","c":[0.1,1.0E-4]}"""
        assertEquals(json, JsonNumberRepair.repair(json))
        val plain = """{"a":1}"""
        assertSame(plain, JsonNumberRepair.repair(plain))
    }
}
