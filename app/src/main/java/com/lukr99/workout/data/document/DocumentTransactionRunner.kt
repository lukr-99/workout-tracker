package com.lukr99.workout.data.document

import com.lukr99.workout.data.TransactionRunner

/** A [TransactionRunner] whose block is one [DocumentDatabase.transaction]: saved whole or not at all. */
class DocumentTransactionRunner(private val database: DocumentDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = database.transaction(block)
}
