package com.lukr99.workout.data

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithEntries(
    @Embedded val session: SessionEntity,
    @Relation(entity = EntryEntity::class, parentColumn = "id", entityColumn = "workoutSessionId")
    val entries: List<EntryWithSets>,
)
