package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 4 -> 5: Run Mode (additive, non-destructive): adds `runs`, `run_points`, `routes` and
 * `route_points`. No strength table is touched. The CREATE statements are copied verbatim from the
 * Room-exported `app/schemas/5.json`, so the migrated schema validates identically to a fresh install.
 */
object Migration0005RunTables : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `runs` (`id` TEXT NOT NULL, `sessionId` TEXT, `startedAtUtc` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `movingSeconds` INTEGER NOT NULL, `distanceMeters` REAL NOT NULL, `avgPaceSecPerKm` REAL NOT NULL, `elevationGainM` REAL NOT NULL, `calories` REAL, `avgHr` INTEGER, `source` INTEGER NOT NULL, `externalKey` TEXT, `encodedPolyline` TEXT NOT NULL, `routeId` TEXT, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_sessionId` ON `runs` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_routeId` ON `runs` (`routeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_startedAtUtc` ON `runs` (`startedAtUtc`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_runs_externalKey` ON `runs` (`externalKey`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `run_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `runId` TEXT NOT NULL, `t` INTEGER NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, `elevationM` REAL, `speedMps` REAL, `hrBpm` INTEGER, `accuracyM` REAL, FOREIGN KEY(`runId`) REFERENCES `runs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_run_points_runId` ON `run_points` (`runId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `distanceMeters` REAL NOT NULL, `elevationGainM` REAL NOT NULL, `encodedPolyline` TEXT NOT NULL, `createdAtUtc` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routes_name` ON `routes` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `route_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routeId` TEXT NOT NULL, `seq` INTEGER NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, `elevationM` REAL, FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_route_points_routeId` ON `route_points` (`routeId`)")
    }
}
