package eu.tintera.guard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface PendingTokenObservable {
    val pendingToken: Flow<Set<Token>>
}

fun Flow<Set<Token>>.asEventStream(): Flow<Token> = flow {

    val processedTags = mutableSetOf<Token>()

    collect { currentSet ->

        val newTokens = currentSet.filter { it !in processedTags }

        newTokens.forEach { token ->
            processedTags.add(token)
            emit(token)
        }

        processedTags.retainAll(currentSet)
    }
}