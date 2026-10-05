package com.lukr99.workout.data

import androidx.room.withTransaction

class RoomTransactionRunner(private val db: WorkoutDb) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
