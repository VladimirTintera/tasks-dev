package eu.tintera.background.tasks.core.data

interface TransactionRunner {
    suspend fun <T> withTransaction(action: suspend () -> T): T
}

suspend operator fun <T> TransactionRunner.invoke(
    action: suspend () -> T
) = withTransaction(action)