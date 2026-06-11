package eu.tintera.guard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun Flow<Token>.autoReleasePrevious(): Flow<Token> = channelFlow {
    var previousToken: Token? = null

    collect { newToken ->

        send(newToken)

        val tokenToRelease = previousToken

        previousToken = newToken

        tokenToRelease?.also { oldToken ->
            launch {
                newToken.state.first { it != TokenState.INITIATED }
                oldToken.release()
            }
        }
    }
}