package eu.tintera.tasks.db

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import eu.tintera.tasks.core.data.TransactionRunner

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