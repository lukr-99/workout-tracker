package com.lukr99.workout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lukr99.workout.data.run.RouteEntity
import com.lukr99.workout.data.run.RoutePointEntity
import com.lukr99.workout.data.run.RunDao
import com.lukr99.workout.data.run.RunEntity
import com.lukr99.workout.data.run.RunPointEntity
import com.lukr99.workout.data.migrations.WorkoutMigrations

/**
 * The app's Room database. Exports its schema to `app/schemas/` (checked in) so future
 * migrations are validated; there is **no destructive fallback** — a schema change without a
 * migration must fail loudly rather than wipe a user's training history.
 *
 * Migrations live one per file in `data/migrations/`, listed in [WorkoutMigrations].
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
        // Run Mode (v5, additive) — see data/run/.
        RunEntity::class,
        RunPointEntity::class,
        RouteEntity::class,
        RoutePointEntity::class,
    ],
    version = 8,
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
                .addMigrations(*WorkoutMigrations.ALL)
                .build()
    }
}
