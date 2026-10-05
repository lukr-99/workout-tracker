package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 5 -> 6: Run Mode segment breaks (additive): `run_points.segmentStart` marks the first point after
 * a manual pause, so a paused-and-walked stretch neither connects on the map nor counts toward
 * distance. Existing points default to 0, one continuous segment, as before.
 */
object Migration0006RunSegmentBreaks : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE run_points ADD COLUMN segmentStart INTEGER NOT NULL DEFAULT 0")
    }
}
