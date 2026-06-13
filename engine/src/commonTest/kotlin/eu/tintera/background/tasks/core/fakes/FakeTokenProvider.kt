package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.guard.AbstractToken
import eu.tintera.background.guard.Token
import eu.tintera.background.guard.TokenProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeTokenProvider : TokenProducer {
    var acquireCount = 0
    var releaseCount = 0
    var cancelCount = 0

    var simulateInstantExpiration = false

    private val activeTokens = mutableListOf<FakeTokenImpl>()

    override fun token(): Flow<Token> = flow {
        acquireCount++

        val token = FakeTokenImpl(
            onReleaseAction = {
                releaseCount++
                activeTokens.remove(it)
            },
            onCancelAction = {
                cancelCount++
                activeTokens.remove(it)
            }
        )

        activeTokens.add(token)

        if (simulateInstantExpiration) {
            token.cancel()
        }

        emit(token)
    }

    fun triggerExpiration() {
        activeTokens.toList().forEach { it.cancel() }
    }
}

class FakeTokenImpl(
    private val onReleaseAction: (FakeTokenImpl) -> Unit,
    private val onCancelAction: (FakeTokenImpl) -> Unit
) : AbstractToken() {

    override val tag: String
        get() = "FakeToken"

    override suspend fun onRelease() {
        onReleaseAction(this)
    }

    override fun onCancel() {
        onCancelAction(this)
    }

    fun cancel() {
        finishWithCancel()
    }
}