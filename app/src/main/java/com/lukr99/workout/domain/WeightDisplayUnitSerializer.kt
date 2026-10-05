package com.lukr99.workout.domain

object WeightDisplayUnitSerializer :
    OrdinalEnumSerializer<WeightDisplayUnit>("WeightDisplayUnit", WeightDisplayUnit.entries.toTypedArray())
