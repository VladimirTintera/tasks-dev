package eu.tintera.tasks

import eu.tintera.guard.Token
import eu.tintera.guard.TokenProvider

internal class JvmTokenProvider : TokenProvider {
    override suspend fun acquire(
        expirationHandler: () -> Unit
    ): Token  = object : Token {

        override suspend fun release() {

        }

        override fun cancel() {

        }

    }
}