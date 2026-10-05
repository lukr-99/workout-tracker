package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 7 -> 8: richer exercise info (additive): how-to steps and an optional guide link. Existing
 * exercises get no steps and no link; their personal notes are untouched.
 */
object Migration0008ExerciseGuides : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN instructions TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercises ADD COLUMN videoUrl TEXT")
    }
}
