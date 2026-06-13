package eu.tintera.guard

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
    private var debounceJob: Job? = null

    private val _multiplexerState = MutableStateFlow(MultiplexerState())
    override val state: StateFlow<MultiplexerState> = _multiplexerState.asStateFlow()

    override suspend fun acquire(): ExecutionContext = mutex.withLock {
        cancelDebounce()

        var session = currentSession.load()

        if (session == null || session.isExpired.value) {
            val expirationFlow = MutableStateFlow(false)
            val newSession = Session(expirationFlow)

            // Získáme systémový zámek ze sjednoceného TokenProducer
            val sysToken = tokenProducer.token().first()

            sysToken.invokeOnPreCancel {
                lifecycleObserver.onPreCancel()
                cancelDebounce()
                newSession.isExpired.value = true
                currentSession.compareAndSet(newSession, null)
                newSession.systemToken.exchange(null)
            }

            newSession.systemToken.store(sysToken)

            // Defenzivní check. Co když callback běžel přesně mezi acquire() a store()?
            // Pokud ano, ukradneme token my a zrušíme ho, aby nezůstal viset v paměti systému.

            if (newSession.isExpired.value) {
                newSession.systemToken.exchange(null)?.release()
            } else {
                _multiplexerState.update { it.copy(isSystemTokenHeld = true) }
                lifecycleObserver.onStarted()
            }

            session = newSession
            currentSession.store(newSession)
        }

        val activeCount = session.activeCount.incrementAndFetch()
        _multiplexerState.update { it.copy(activeTasksCount = activeCount) }

        return ExecutionContextImpl(session, ::releaseSessionToken)
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
                    runImmediateTeardown = true // Jen si poznačíme, že máme uklízet
                }
            }
        }

        if (runImmediateTeardown) {
            performTeardown(session)
        }
    }

    private suspend fun performTeardown(session: Session) {
        // 1. Atomicky zjistíme, jestli jsme "vítězové", kdo má uklidit.
        // Zámek držíme doslova jen na pár nanosekund.
        val isWinnerToTeardown = mutex.withLock {
            currentSession.compareAndSet(session, null)
        }

        // 2. Pokud jsme vyhráli, jdeme uklízet. Ale už BEZ ZÁMKU!
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