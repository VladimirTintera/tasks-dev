package eu.tintera.tasks

import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProvider

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