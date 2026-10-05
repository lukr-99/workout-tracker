package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration

/**
 * The index of Room migrations, in order. Each lives in its own file named by the schema version it
 * produces (`Migration0008...` takes 7 to 8). A migration is never edited once released: a schema
 * change adds the next file here, its exported schema JSON, an isolated N-1 -> N test and the full
 * chain test (CodePrint data lifecycle).
 */
object WorkoutMigrations {
    val ALL: Array<Migration> = arrayOf(
        Migration0002SessionSource,
        Migration0003ExerciseImageUrl,
        Migration0004ExercisePhotoPath,
        Migration0005RunTables,
        Migration0006RunSegmentBreaks,
        Migration0007LiveLoggingMetadata,
        Migration0008ExerciseGuides,
    )
}
