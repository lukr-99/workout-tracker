package com.lukr99.workout.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's Room database (schema v1). Exports its schema to `app/schemas/` (checked in) so future
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
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDb : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DB_NAME = "workout.db"

        fun build(context: Context): WorkoutDb =
            Room.databaseBuilder(context.applicationContext, WorkoutDb::class.java, DB_NAME)
                .build()
    }
}
