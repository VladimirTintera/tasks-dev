package eu.tintera.background.tasks.db

import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import eu.tintera.background.tasks.core.data.TransactionRunner

internal class DatabaseTransactionRunner(
    private val database: TasksDatabase
) : TransactionRunner {

    override suspend fun <T> withTransaction(
        action: suspend () -> T
    ): T = database.useWriterConnection {
        it.immediateTransaction {
            action()
        }
    }
}