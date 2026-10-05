package com.lukr99.workout.data

import androidx.room.Embedded
import androidx.room.Relation

data class EntryWithSets(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutEntryId")
    val strengthSets: List<StrengthSetEntity>,
    @Relation(parentColumn = "id", entityColumn = "workoutEntryId")
    val cardio: CardioDataEntity?,
)
