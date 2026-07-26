package com.lukr99.workout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDb : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DB_NAME = "workout.db"

        fun build(context: Context): WorkoutDb =
            Room.databaseBuilder(context.applicationContext, WorkoutDb::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2)
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
    }
}
