package com.lukr99.workout.data

/** Keeps atomic orchestration testable without exposing Room outside the data layer. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
