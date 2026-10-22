package com.lukr99.workout.domain

object LocationKindSerializer :
    OrdinalEnumSerializer<LocationKind>("LocationKind", LocationKind.entries.toTypedArray())
