package com.lukr99.workout.domain

object SetTagSerializer :
    OrdinalEnumSerializer<SetTag>("SetTag", SetTag.entries.toTypedArray())
