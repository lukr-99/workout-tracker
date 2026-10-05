package com.lukr99.workout.domain

object SetTypeSerializer :
    OrdinalEnumSerializer<SetType>("SetType", SetType.entries.toTypedArray())
