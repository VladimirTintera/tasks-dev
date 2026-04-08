package eu.tintera.tasks.core

import eu.tintera.tasks.State
import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProducer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Manages the application lifecycle events on iOS and coordinates task recovery.
 *
 * This class listens for UIKit lifecycle notifications to track background/foreground states
 * and ensures that tasks stuck in a [State.Running] state are reset when the app resumes.
 */
internal class UiBackgroundTaskTokenProducer(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val appLifecycleObserver: AppLifecycleObserver
) : TokenProducer {

    override fun token(onExpire: () -> Unit): Flow<Token> = flow {
        val token = UiBackgroundTaskToken(
            scope = scope,
            dispatcher = dispatcher,
            expirationHandler = onExpire,
            isBackground = appLifecycleObserver.isBackground
        )
        emit(token)
    }
}