package com.lukr99.workout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lukr99.workout.data.run.RouteEntity
import com.lukr99.workout.data.run.RoutePointEntity
import com.lukr99.workout.data.run.RunDao
import com.lukr99.workout.data.run.RunEntity
import com.lukr99.workout.data.run.RunPointEntity

/**
 * The app's Room database. Exports its schema to `app/schemas/` (checked in) so future
 * migrations are validated; there is **no destructive fallback** — a schema change without a
 * migration must fail loudly rather than wipe a user's training history.
 *
 * Foreign-key enforcement is on by default in Room. Seeding happens once, on first run, via
 * [WorkoutRepository.ensureSeeded] (mirrors the MAUI `InitializeAsync`).
 */
@Database(
    entities = [
        ExerciseEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class,
        SessionEntity::class,
        EntryEntity::class,
        StrengthSetEntity::class,
        CardioDataEntity::class,
        // Run Mode (v5, additive) — see data/run/RunEntities.kt.
        RunEntity::class,
        RunPointEntity::class,
        RouteEntity::class,
        RoutePointEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDb : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    abstract fun runDao(): RunDao

    companion object {
        private const val DB_NAME = "workout.db"

        fun build(context: Context): WorkoutDb =
            Room.databaseBuilder(context.applicationContext, WorkoutDb::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN source INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sessions ADD COLUMN externalKey TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sessions_externalKey ON sessions(externalKey)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageAttribution TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN localImagePath TEXT")
            }
        }

        /**
         * v5 — Run Mode (additive, non-destructive): adds `runs`, `run_points`, `routes`,
         * `route_points`. No strength table is touched. The CREATE statements below are copied
         * verbatim from the Room-exported `app/schemas/5.json` so the migrated schema validates
         * identically to a fresh install.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
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
    }
}
