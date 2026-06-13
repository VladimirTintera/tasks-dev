package eu.tintera.background.guard

import kotlinx.coroutines.flow.SharedFlow

interface TokenObservable {
    val acquiredTokens: SharedFlow<Token>
}