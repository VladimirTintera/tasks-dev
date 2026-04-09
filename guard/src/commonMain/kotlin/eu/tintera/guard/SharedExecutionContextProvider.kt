package eu.tintera.guard

import eu.tintera.guard.EventBus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [ExecutionContextProvider] that allows multiple [ExecutionContext]s
 * to share a single underlying system [Token].
 *
 * It manages a [Session] that tracks active consumers. When the first consumer requests
 * a context, a system token is acquired. Subsequent requests share this token until
 * the session expires or all consumers release their contexts.
 */
class SharedExecutionContextProvider(
    private val tokenProvider: TokenProvider,
    private val scope: CoroutineScope,
    private val config: ExecutionContextConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val lifecycleObserver: ExecutionContextObserver? = null
) : ExecutionContextProvider {
    private val mutex = Mutex()
    private val currentSession = AtomicReference<Session?>(null)
    private var debounceJob: Job? = null // ZDE JE ZMĚNA

    /**
     * Acquires an [ExecutionContext]. If a valid session exists, it increments its consumer count.
     * Otherwise, it initiates a new session by acquiring a system [Token].
     */
    override suspend fun acquire(): ExecutionContext = mutex.withLock {

        cancelDebounce()

        var session = currentSession.load()

        if (session == null || session.isExpired.value) {
            val expirationFlow = MutableStateFlow(false)
            val newSession = Session(expirationFlow)

            // Získáme systémový zámek
            val sysToken = tokenProvider.acquire {
                EventBus.send(TAG, "token expired")
                cancelDebounce()
                // EXPIRATION HANDLER (volán systémem, když dochází čas)
                newSession.isExpired.value = true

                // Pokud je tato session stále ta "aktuální", odstraníme ji, aby další acquire založil novou
                currentSession.compareAndSet(newSession, null)

                lifecycleObserver?.onPreCancel()
                // Zrušíme systémový token (pokud už byl nastaven)
                newSession.systemToken.exchange(null)?.cancel()
            }

            newSession.systemToken.store(sysToken)

            // Defenzivní check. Co když callback běžel přesně mezi acquire() a store()?
            // Pokud ano, ukradneme token my a zrušíme ho, aby nezůstal viset v paměti systému.

            if (newSession.isExpired.value) {
                newSession.systemToken.exchange(null)?.release()
            } else {
                lifecycleObserver?.onStarted()
            }

            session = newSession
            currentSession.store(newSession)
        }

        // Bezpečně inkrementujeme počítadlo v rámci této konkrétní session
        session.activeCount.incrementAndFetch()

        return ExecutionContextImpl(session, ::releaseSessionToken)
    }

    private fun cancelDebounce() {
        debounceJob?.also {
            EventBus.send(TAG, "debounce job canceled")
            it.cancel()
        }
        debounceJob = null
    }

    private suspend fun releaseSessionToken(session: Session) = withContext(NonCancellable) {
        mutex.withLock {
            // Snížíme počítadlo u dané session
            val newCount = session.activeCount.decrementAndFetch()

            // Pokud jsme na nule A ZÁROVEŇ se nám podaří tuto session odstranit z globálního stavu
            if (newCount == 0) {

                // FAST-PATH PRO EXPIROVANOU SESSION ---
                // Pokud už nám systém zabil task, expiration handler udělal veškerý úklid.
                // Nemá absolutně smysl startovat debounce nebo čekat na PreRelease.
                if (session.isExpired.value) {
                    EventBus.send(TAG, "Session is already expired. Skipping debounce.")
                    return@withLock
                }

                debounceJob = scope.launch(dispatcher) {
                    if (config.releaseDebounce.isPositive()) {
                        EventBus.send(TAG, "debounce delay started")
                        delay(config.releaseDebounce)
                    }

                    mutex.withLock {
                        // Pokud jsme na nule, zkusíme tuto session odstranit z "currentSession"
                        // Pokud se to podaří, jsme zodpovědní za uvolnění systémového zámku.
                        // Pokud se to nepodaří (protože už ji odstranil expiration handler nebo jiný release),
                        // tak už nemáme co dělat.
                        if (currentSession.compareAndSet(session, null)) {
                            withTimeoutOrNull(2.seconds) {
                                lifecycleObserver?.onPreRelease()
                            }

                            // Jsme vítězové, kdo zavírá dveře. Uvolníme systémový zámek.
                            session.systemToken.exchange(null)?.release()
                        }
                    }
                }
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

    companion object {
        private const val TAG = "SharedExecutionContextProvider"
    }
}