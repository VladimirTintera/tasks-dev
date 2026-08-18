package eu.tintera.background.guard

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Duration.Companion.seconds

internal class SharedExecutionContextProvider(
    private val tokenProducer: TokenProducer,
    private val scope: CoroutineScope,
    private val config: ExecutionEnvironmentConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val lifecycleObserver: ExecutionContextObserver
) : ExecutionContextProvider, MultiplexerObservable {
    private val mutex = Mutex()
    private val currentSession = AtomicReference<Session?>(null)
    private var activeSessionDeferred: Deferred<Session>? = null
    private var debounceJob: Job? = null

    private val _multiplexerState = MutableStateFlow(MultiplexerState())
    override val state: StateFlow<MultiplexerState> = _multiplexerState.asStateFlow()

    override suspend fun acquire(): ExecutionContext {
        val session = getOrCreateSession()
        return ExecutionContextImpl(session, ::releaseSessionToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getOrCreateSession(): Session {
        var joined = false
        val deferred = mutex.withLock {
            cancelDebounce()
            val current = currentSession.load()
            if (current != null && !current.isExpired.value) {
                val activeCount = current.activeCount.incrementAndFetch()
                _multiplexerState.update { it.copy(activeTasksCount = activeCount) }
                return@withLock CompletableDeferred(current)
            }

            val existingDeferred = activeSessionDeferred
            if (existingDeferred != null && !existingDeferred.isCompleted) {
                joined = true
                existingDeferred
            } else {
                val newDeferred = scope.async(dispatcher) {
                    try {
                        val expirationFlow = MutableStateFlow(false)
                        val session = Session(expirationFlow, activeCount = AtomicInt(1))

                        val sysToken = tokenProducer.token().first()

                        sysToken.invokeOnPreCancel {
                            lifecycleObserver.onPreCancel()
                            cancelDebounce()
                            session.isExpired.value = true
                            currentSession.compareAndSet(session, null)
                            session.systemToken.exchange(null)
                        }

                        session.systemToken.store(sysToken)

                        if (session.isExpired.value) {
                            session.systemToken.exchange(null)?.release()
                        } else {
                            _multiplexerState.update {
                                it.copy(
                                    isSystemTokenHeld = true,
                                    activeTasksCount = it.activeTasksCount + 1
                                )
                            }
                            currentSession.store(session)
                            lifecycleObserver.onStarted()
                        }

                        session
                    } catch (e: Throwable) {
                        mutex.withLock {
                            if (activeSessionDeferred === this@async) {
                                activeSessionDeferred = null
                            }
                        }
                        throw e
                    }
                }
                activeSessionDeferred = newDeferred
                newDeferred
            }
        }

        try {
            if (joined) {
                val session = deferred.await()
                val activeCount = session.activeCount.incrementAndFetch()
                _multiplexerState.update { it.copy(activeTasksCount = activeCount) }
                return session
            } else {
                return deferred.await()
            }
        } catch (e: Throwable) {
            mutex.withLock {
                if (activeSessionDeferred === deferred) {
                    activeSessionDeferred = null
                }
            }
            try {
                val session = deferred.getCompleted()
                releaseSessionToken(session)
            } catch (_: Throwable) {
            }
            throw e
        }
    }


    private fun cancelDebounce() {
        debounceJob?.also {
            it.cancel()
        }
        debounceJob = null
        _multiplexerState.update { it.copy(isDebouncing = false) }
    }

    private suspend fun releaseSessionToken(session: Session) = withContext(NonCancellable) {
        var runImmediateTeardown = false

        mutex.withLock {
            val newCount = session.activeCount.decrementAndFetch()
            _multiplexerState.update { it.copy(activeTasksCount = newCount) }
            if (newCount == 0) {
                if (session.isExpired.value) return@withLock

                if (config.releaseDebounce.isPositive()) {
                    _multiplexerState.update { it.copy(isDebouncing = true) }
                    debounceJob = scope.launch(dispatcher) {
                        delay(config.releaseDebounce)
                        performTeardown(session)
                    }
                } else {
                    runImmediateTeardown = true // just note that teardown is due
                }
            }
        }

        if (runImmediateTeardown) {
            performTeardown(session)
        }
    }

    private suspend fun performTeardown(session: Session) {
        // 1. Atomically decide whether we are the one that has to tear down.
        //    The lock is held for just a few nanoseconds.
        val isWinnerToTeardown = mutex.withLock {
            currentSession.compareAndSet(session, null)
        }

        // 2. If we won, tear down — WITHOUT holding the lock.
        if (isWinnerToTeardown) {
            withTimeoutOrNull(2.seconds) {
                lifecycleObserver.onPreRelease()
            }

            session.systemToken.exchange(null)?.release()
            _multiplexerState.update {
                it.copy(
                    isSystemTokenHeld = false,
                    isDebouncing = false,
                )
            }
        }
    }

    private class Session(
        val isExpired: MutableStateFlow<Boolean>,
        val activeCount: AtomicInt = AtomicInt(0),
        val systemToken: AtomicReference<Token?> = AtomicReference(null)
    )

    private class ExecutionContextImpl(
        private val session: Session,
        private val releaseAction: suspend (Session) -> Unit
    ) : ExecutionContext {
        override val isExpired: StateFlow<Boolean> = session.isExpired.asStateFlow()

        override suspend fun release() {
            releaseAction(session)
        }
    }
}