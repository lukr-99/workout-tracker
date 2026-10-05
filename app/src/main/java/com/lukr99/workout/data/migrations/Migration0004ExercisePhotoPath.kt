package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 3 -> 4: the path of a personal exercise photo in the app's files. */
object Migration0004ExercisePhotoPath : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN localImagePath TEXT")
    }
}
