package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 2 -> 3: a remote image and its attribution on catalog exercises. */
object Migration0003ExerciseImageUrl : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageUrl TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageAttribution TEXT")
    }
}
