package eu.tintera.guard

import eu.tintera.guard.fakes.SpyExecutionContextObserver
import eu.tintera.guard.fakes.FakeTokenProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val defaultReleaseDebounce = 1.5.seconds

internal fun TestScope.executionContextProvider(
    tokenProducer: TokenProducer,
    releaseDebounce: Duration = defaultReleaseDebounce
) = SharedExecutionContextProvider(
    tokenProducer = tokenProducer,
    scope = CoroutineScope(SupervisorJob()),
    dispatcher = StandardTestDispatcher(testScheduler),
    config = ExecutionEnvironmentConfig(releaseDebounce),
    lifecycleObserver = object : ExecutionContextObserver {}
)

class SharedExecutionContextProviderTest {

    @Test
    fun `when first token is acquired system lock is acquired`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token = wakeLock.acquire()

        assertEquals(1, fakeSystemLock.acquireCount)
        assertEquals(0, fakeSystemLock.releaseCount)
        assertFalse(token.isExpired.value)
    }

    @Test
    fun `when second token is acquired system lock is NOT acquired again`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        wakeLock.acquire()
        wakeLock.acquire()

        assertEquals(1, fakeSystemLock.acquireCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when acquire is called during debounce system lock is not released`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        token1.release() // Začíná běžet 1.5s odpočet

        // Posuneme čas před vypršení release debouncu - zámek by měl stále držet
        advanceTimeBy(defaultReleaseDebounce - 1.milliseconds)

        assertEquals(0, fakeSystemLock.releaseCount)

        // Přijde další zájemce, debounce se musí zrušit
        wakeLock.acquire()

        // Posuneme čas o další 2 sekundy. Kdyby debounce běžel, teď by to uvolnil.
        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)

        // Zámek musí stále držet, protože ho drží token2!
        assertEquals(0, fakeSystemLock.releaseCount)
    }


    @Test
    fun `when all tokens are released system lock is released`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        val token2 = wakeLock.acquire()

        token1.release()
        assertEquals(0, fakeSystemLock.releaseCount, "Released count must be 0")

        token2.release()

        advanceTimeBy(defaultReleaseDebounce - 1.milliseconds)

        assertEquals(0, fakeSystemLock.releaseCount, "Released count must still be 1 (debounce)")

        advanceTimeBy(10.minutes)

        assertEquals(1, fakeSystemLock.releaseCount, "Released count must be 1")
    }

    @Test
    fun `when system lock expires all tokens are marked expired`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        val token2 = wakeLock.acquire()

        // Simulate expiration
        fakeSystemLock.triggerExpiration()

        assertTrue(token1.isExpired.value)
        assertTrue(token2.isExpired.value)

        // Also check that cancel was called on the system lock
        assertEquals(1, fakeSystemLock.cancelCount)
    }

    @Test
    fun `when expired releasing tokens does not crash or double release`() = runTest {

        val tokenProvider = FakeTokenProvider()
        val executionContextProvider = executionContextProvider(tokenProvider)

        val executionContext = executionContextProvider.acquire()

        tokenProvider.triggerExpiration()

        // Release after expiration should be safe and ignored
        executionContext.release()

        // Release count should still be 0 because it was cancelled, not released normally
        assertEquals(0, tokenProvider.releaseCount)
        assertEquals(1, tokenProvider.cancelCount)
    }

    @Test
    fun `after expiration, new acquire starts fresh`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        val token1 = wakeLock.acquire()
        fakeSystemLock.triggerExpiration()

        // Old token is expired
        assertTrue(token1.isExpired.value, "Token must be expired")

        // New acquire should start a new system lock
        val token2 = wakeLock.acquire()

        assertEquals(2, fakeSystemLock.acquireCount) // 1st expired, 2nd started
        assertFalse(token2.isExpired.value)

        token2.release()

        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)

        assertEquals(1, fakeSystemLock.releaseCount) // Only the second one was released normally
    }

    @Test
    fun `orphaned token release does not affect new session`() = runTest {
        val fakeSystemLock = FakeTokenProvider()
        val wakeLock = executionContextProvider(fakeSystemLock)

        // 1. Acquire first token
        val tokenA = wakeLock.acquire()
        assertEquals(1, fakeSystemLock.acquireCount)

        // 2. Simulate expiration, making tokenA an orphan
        fakeSystemLock.triggerExpiration()
        assertTrue(tokenA.isExpired.value)
        assertEquals(1, fakeSystemLock.cancelCount, "Canceled count must be 1")

        // 3. Acquire a new token, creating a new session
        val tokenB = wakeLock.acquire()
        assertEquals(2, fakeSystemLock.acquireCount)
        assertFalse(tokenB.isExpired.value)

        // 4. Release the orphaned tokenA
        tokenA.release()

        // 5. CRUCIAL: Assert that the new system lock was NOT released
        assertEquals(0, fakeSystemLock.releaseCount)

        // 6. Release the valid tokenB
        tokenB.release()

        // 7. Assert that the new system lock is now correctly released
        advanceTimeBy(defaultReleaseDebounce + 1.milliseconds)
        assertEquals(1, fakeSystemLock.releaseCount)
    }

    @Test
    fun `when system expires lock instantly during acquire, token is cancelled immediately`() = runTest {
        // Arrange
        val fakeSystemLock = FakeTokenProvider().apply {
            simulateInstantExpiration = true // Zapínáme zákeřný scénář
        }
        val wakeLock = executionContextProvider(fakeSystemLock)

        // Act
        // Požádáme o zámek. Vlivem flagu expiruje dřív, než se funkce dokončí.
        val token = wakeLock.acquire()

        // Assert
        assertEquals(1, fakeSystemLock.acquireCount, "Měl by proběhnout 1 dotaz na systém")

        // Protože expiration handler proběhl DŘÍV, než se uložil token do session,
        // callback token nemohl zrušit.
        // Zrušil ho až defenzivní check po uložení!
        assertEquals(0, fakeSystemLock.releaseCount, "Defenzivní check nemůže volat release na již zrušeném tokenu")
        assertEquals(1, fakeSystemLock.cancelCount, "Standardní cancel se volá při expiraci")

        // Vrácený token musí pochopitelně rovnou hlásit, že je expirovaný
        assertTrue(token.isExpired.value, "Token musí být okamžitě ve stavu expirace")

        // Act 2
        // Pokud by se teď zavolal release,
        // nemělo by se už dít vůbec nic, protože token už je zrušený.
        token.release()

        // Assert 2
        assertEquals(0, fakeSystemLock.releaseCount, "Počet release by měl zůstat 0")
        assertEquals(1, fakeSystemLock.cancelCount, "Cancel na zrušeném tokenu nesmí volat systémový cancel znovu")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when coroutine is cancelled, system wake lock is still released`() = runTest {
        // 1. Příprava
        val fakePlatformProvider = FakeTokenProvider()
        val countingProvider = executionContextProvider(
            tokenProducer = fakePlatformProvider,
            releaseDebounce = Duration.ZERO
        )

        // 2. Spustíme job, který si vyžádá zámek a pak "usne" (simulace práce)
        val job = launch {
            val token = countingProvider.acquire()
            token.use {
                // Tady coroutina normálně pracuje...
                // My ji tu úmyslně uspíme, abychom ji mohli zvenčí zrušit
                delay(5000)
            }
        }

        // Posuneme čas, aby se coroutina rozběhla a stihla si vyžádat zámek
        runCurrent()

        // Ověříme, že zámek byl na úrovni systému opravdu vyžádán
        assertEquals(1, fakePlatformProvider.acquireCount, "Acquire by měl být zavolán 1x")
        assertEquals(0, fakePlatformProvider.releaseCount, "Release by zatím NEMĚL být zavolán")

        // 3. AKCE: Natvrdo zrušíme coroutinu (tohle simuluje např. expiraci nebo smazání tasku)
        job.cancelAndJoin()

        // 4. ASSERT: Tohle je ten kritický bod!
        // Pokud chybí withContext(NonCancellable), test tady spadne, protože release bude 0.
        assertEquals(1, fakePlatformProvider.releaseCount, "Release MUSÍ být zavolán i po zrušení coroutiny!")
    }

    @Test
    fun `when system expires task immediately during acquire, token is cancelled`() = runTest {
        val fakeProvider = FakeTokenProvider().apply {
            simulateInstantExpiration = true
        }
        val countingProvider = executionContextProvider(fakeProvider)

        // Akce
        val token = countingProvider.acquire()

        // Assert: Zámek musí vědět, že je mrtvý
        assertTrue(token.isExpired.value, "Token by měl být rovnou expirovaný")

        // Assert: Náš defenzivní kód musel zrušit neplatný token
        assertEquals(0, fakeProvider.releaseCount)
        assertEquals(1, fakeProvider.cancelCount)
    }

    @Test
    fun `uspesny acquire a release zavola onStarted a po debounce onPreRelease`() = runTest {
        // Arrange
        // Použijeme StandardTestDispatcher, abychom měli plnou kontrolu nad virtuálním časem
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver()

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope, // Poskytuje runTest, automaticky se uklidí
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        // Act - Start
        val executionContext = contextProvider.acquire()

        // Assert - Start
        assertEquals(1, observer.startedCount, "onStarted melo byt zavolano presne jednou")
        assertEquals(0, tokenProvider.releaseCount)

        // Act - Zpracování a uvolnění (Release)
        executionContext.release()

        // Okamžitě po release by observer ještě neměl dostat onPreRelease (kvůli debounce)
        assertEquals(0, observer.preReleaseCount)
        assertEquals(0, tokenProvider.releaseCount)

        // Posuneme čas o debounce limit (5 vteřin)
        advanceTimeBy(5001)

        // Assert - Konec
        assertEquals(1, observer.preReleaseCount, "onPreRelease melo byt zavolano po uplynuti debounce")
        assertEquals(1, tokenProvider.releaseCount, "Systemovy token mel byt na konci uvolnen")
    }

    @Test
    fun `okamzita expirace systemu zavola onPreCancel a zrusi token`() = runTest {
        // Arrange
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver()

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        // Nastartujeme kontext
        contextProvider.acquire()
        assertEquals(1, observer.startedCount)

        // Act - Systém náhle oznámí vypršení času
        tokenProvider.triggerExpiration()

        // Assert
        assertEquals(1, observer.preCancelCount, "onPreCancel melo byt okamzite zavolano")
        assertEquals(0, observer.preReleaseCount, "onPreRelease se pri expiraci volat nesmi")
        assertEquals(1, tokenProvider.cancelCount, "Systemovy token mel byt zrusen (cancel)")
        assertEquals(0, tokenProvider.releaseCount)
    }

    @Test
    fun `pokud onPreRelease trva dele nez timeout pojistka systemovy token se stejne uvolni`() = runTest {
        // Arrange
        val tokenProvider = FakeTokenProvider()
        val observer = SpyExecutionContextObserver().apply {
            // Observer se zasekne na 10 vteřin (naše withTimeoutOrNull je ale na 2 vteřiny!)
            delayInPreRelease = 10.seconds
        }

        val contextProvider = SharedExecutionContextProvider(
            tokenProducer = tokenProvider,
            scope = backgroundScope,
            dispatcher = StandardTestDispatcher(testScheduler),
            config = ExecutionEnvironmentConfig(releaseDebounce = 5.seconds),
            lifecycleObserver = observer
        )

        val executionContext = contextProvider.acquire()
        executionContext.release()

        // Posuneme čas o 5 vteřin (vyprší debounce) a k tomu 2 vteřiny (vyprší withTimeoutOrNull uvnitř provideru)
        advanceTimeBy((5000 + 2001).milliseconds)

        // Assert
        assertEquals(1, observer.preReleaseCount, "Observeruv onPreRelease mel byt spusten")
        assertEquals(
            1,
            tokenProvider.releaseCount,
            "Systemovy token MUSI byt uvolnen bez ohledu na to, ze se observer zasekl"
        )
    }
}

