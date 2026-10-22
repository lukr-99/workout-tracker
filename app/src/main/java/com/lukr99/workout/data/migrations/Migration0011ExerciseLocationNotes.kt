package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 10 -> 11: machine notes per exercise and location (additive). One new table, no change to any
 * existing row, so every workout, exercise and location reads exactly as before.
 */
object Migration0011ExerciseLocationNotes : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `exercise_location_notes` (`exerciseId` TEXT NOT NULL, " +
                "`locationId` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`exerciseId`, `locationId`))",
        )
    }
}
