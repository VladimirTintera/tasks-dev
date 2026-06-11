package eu.tintera.guard

import kotlinx.coroutines.flow.SharedFlow

interface TokenObservable {
    val acquiredTokens: SharedFlow<Token>
}