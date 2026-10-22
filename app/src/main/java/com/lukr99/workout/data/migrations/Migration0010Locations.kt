package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 9 -> 10: training locations (additive). A new `locations` table with the built-in Home and
 * Outdoors rows, a nullable `sessions.locationId`, and two exercise flags. Every existing workout
 * keeps a null location ("Unknown location") and every exercise stays shared, so all numbers read
 * exactly as before until the owner names a gym.
 */
object Migration0010Locations : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `locations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`kind` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `isArchived` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        // Same ids and kinds as Location.builtIns (Home=1, Outdoors=2).
        db.execSQL("INSERT OR IGNORE INTO locations (id, name, kind, latitude, longitude, isArchived) VALUES ('home', 'Home', 1, NULL, NULL, 0)")
        db.execSQL("INSERT OR IGNORE INTO locations (id, name, kind, latitude, longitude, isArchived) VALUES ('outdoors', 'Outdoors', 2, NULL, NULL, 0)")
        db.execSQL("ALTER TABLE sessions ADD COLUMN locationId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_locationId` ON `sessions` (`locationId`)")
        db.execSQL("ALTER TABLE exercises ADD COLUMN trackPerLocation INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercises ADD COLUMN splitPromptMuted INTEGER NOT NULL DEFAULT 0")
    }
}
