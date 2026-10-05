package com.lukr99.workout.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 1 -> 2: where a workout came from (local or an import) and its external key. */
object Migration0002SessionSource : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN source INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sessions ADD COLUMN externalKey TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_externalKey ON sessions(externalKey)")
    }
}
