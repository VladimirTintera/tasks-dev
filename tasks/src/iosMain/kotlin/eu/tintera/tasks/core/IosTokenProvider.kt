package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProvider

internal class IosTokenProvider(
    private val bgTaskManager: BgTaskManager,
    private val appLifecycleManager: AppLifecycleManager
) : TokenProvider {

    override suspend fun acquire(
        expirationHandler: () -> Unit
    ): Token {

        val token = bgTaskManager.createExpirationToken(
            onExpire = expirationHandler
        ) ?: appLifecycleManager.createExpirationToken(
            onExpire = expirationHandler
        )

        EventBus.send(TAG, "Token acquired $token")
        return token
    }

    companion object {
        private const val TAG = "IosTokenProvider"
    }
}