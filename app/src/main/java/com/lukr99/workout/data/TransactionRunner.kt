package com.lukr99.workout.data

import androidx.room.withTransaction

/** Keeps atomic orchestration testable without exposing Room outside the data layer. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

object DirectTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

class RoomTransactionRunner(private val db: WorkoutDb) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}
