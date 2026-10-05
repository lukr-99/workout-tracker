package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 8 -> 9: a plan per template exercise (additive): target sets, a rep range, rest, and a superset
 * group. Existing templates get no plan, so they start workouts exactly as before.
 */
object Migration0009TemplatePlan : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN targetSets INTEGER")
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN repsMin INTEGER")
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN repsMax INTEGER")
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN restSeconds INTEGER")
        db.execSQL("ALTER TABLE template_exercises ADD COLUMN supersetGroup INTEGER")
    }
}
