package eu.tintera.tasks.core.fakes

import eu.tintera.guard.Token
import eu.tintera.guard.TokenProvider

class FakeTokenProvider : TokenProvider {
    var acquireCount = 0
    var releaseCount = 0
    var cancelCount = 0

    var simulateInstantExpiration = false

    private var expirationHandler: (() -> Unit)? = null
    private val activeTokens = mutableListOf<Token>()

    override suspend fun acquire(expirationHandler: () -> Unit): Token {
        acquireCount++
        this.expirationHandler = expirationHandler

        val token = object : Token {
            override suspend fun release() {
                releaseCount++
                activeTokens.remove(this)
            }

            override fun cancel() {
                cancelCount++
                activeTokens.remove(this)
            }

        }
        activeTokens.add(token)

        if (simulateInstantExpiration) {
            // BUM! Operační systém nám vzal čas přesně v milisekundě,
            // kdy jsme o něj požádali, JEŠTĚ PŘEDTÍM než stihneme vrátit token.
            expirationHandler()
        }

        return token
    }

    fun triggerExpiration() {
        expirationHandler?.invoke()
    }
}