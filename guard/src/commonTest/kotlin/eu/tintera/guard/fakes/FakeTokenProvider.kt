package eu.tintera.guard.fakes

import eu.tintera.guard.AbstractToken
import eu.tintera.guard.Token
import eu.tintera.guard.TokenProvider

class FakeTokenProvider : TokenProvider {
    var acquireCount = 0
    var releaseCount = 0
    var cancelCount = 0

    var simulateInstantExpiration = false

    private var expirationHandler: (() -> Unit)? = null
    private val activeTokens = mutableListOf<Token>()

    override suspend fun acquire(
        onPreCancel: () -> Unit,
        onCancel: () -> Unit)
    : Token {
        acquireCount++

        val token = object : AbstractToken() {
            override val tag: String
                get() = "FakeToken"

            override suspend fun onRelease() {
                releaseCount++
                activeTokens.remove(this)
            }

            override fun onCancel() {
                cancelCount++
                activeTokens.remove(this)
            }

            fun cancel() {
                finishWithCancel()
            }

        }

        activeTokens.add(token)

        if (simulateInstantExpiration) {
            // BUM! Operační systém nám vzal čas přesně v milisekundě,
            // kdy jsme o něj požádali, JEŠTĚ PŘEDTÍM než stihneme vrátit token.
            token.cancel()
        }

        return token
    }

    fun triggerExpiration() {
        expirationHandler?.invoke()
    }
}