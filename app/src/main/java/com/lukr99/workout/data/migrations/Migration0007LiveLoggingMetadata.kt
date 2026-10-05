package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 6 -> 7: live-logging metadata (additive): a per-exercise weight unit, start and finish times, and
 * combinable set tags. Existing single set types stay; an empty tag list tells the domain layer to
 * derive the matching tag from them.
 */
object Migration0007LiveLoggingMetadata : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN weightUnitOverride INTEGER")
        db.execSQL("ALTER TABLE entries ADD COLUMN startedAtUtc INTEGER")
        db.execSQL("ALTER TABLE entries ADD COLUMN completedAtUtc INTEGER")
        db.execSQL("ALTER TABLE strength_sets ADD COLUMN tagsJson TEXT NOT NULL DEFAULT '[]'")
    }
}
