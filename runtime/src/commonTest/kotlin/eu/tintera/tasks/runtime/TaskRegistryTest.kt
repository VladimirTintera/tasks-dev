package eu.tintera.tasks.runtime

import eu.tintera.tasks.Tag
import eu.tintera.tasks.core.TagRegistration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRegistryTest {

    // Jednoduchý FakeClock pro manuální posun času v testech
    class FakeClock(var currentTime: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
        override fun now(): Instant = currentTime
        fun advanceBy(duration: Duration) {
            currentTime += duration
        }
    }

    private lateinit var clock: FakeClock
    private lateinit var registry: TaskRegistry

    @BeforeTest
    fun setup() {
        clock = FakeClock()
        // Ideální je mít TaskRegistry jako třídu, ne object,
        // aby se mezi testy nesdílel stav (případně přidat metodu clear()).
        registry = TaskRegistry(clock = clock)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `resolve missing tag starts 5s warmup and returns null when time expires`() = runTest {
        // Arrange
        var result: TagRegistration<*>? = null

        // Act: Spustíme resolve v oddělené coroutině (protože bude suspendovat)
        val job = launch {
            result = registry.resolveTag<MyDummyTag>("missing_tag")
        }

        // Assert: Po 2 sekundách se nic neděje, coroutina stále běží
        advanceTimeBy(2.seconds)
        clock.advanceBy(2.seconds)
        assertTrue(job.isActive)
        assertNull(result)

        // Assert: Po dalších 3 sekundách (celkem 5s) timeout vyprší
        advanceTimeBy(3.seconds)
        clock.advanceBy(3.seconds)

        // Coroutina by měla úspěšně skončit a vrátit null
        job.join()
        assertNull(result)
    }

    @Test
    fun `concurrent resolve requests share the same warmup window`() = runTest {
        // Arrange
        var result1: TagRegistration<*>? = null
        var result2: TagRegistration<*>? = null

        // Act 1: První požadavek odstartuje warmup v čase 0s
        val job1 = launch { result1 = registry.resolveTag<MyDummyTag>("tag_1") }

        // Posuneme čas o 3 sekundy
        advanceTimeBy(3.seconds)
        clock.advanceBy(3.seconds)

        // Act 2: Druhý požadavek přijde v čase 3s (zbývají jen 2 sekundy do konce warmupu!)
        val job2 = launch { result2 = registry.resolveTag<MyDummyTag>("tag_2") }

        // Posuneme čas o zbylé 2 sekundy (celkem 5s od začátku)
        advanceTimeBy(2.seconds)
        clock.advanceBy(2.seconds)

        // Assert: OBA joby musí skončit přesně teď.
        // Job 2 nečekal 5 sekund, svezl se v okně Jobu 1.
        job1.join()
        job2.join()
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun `after warmup is consumed, resolving missing tags is instant`() = runTest {
        // Arrange: Úmyslně vyčerpáme warmup
        val job = launch { registry.resolveTag<MyDummyTag>("trigger_warmup") }
        advanceTimeBy(5.seconds)
        clock.advanceBy(5.seconds)
        job.join()

        // Act: Zkusíme vyhledat další neexistující tag a změříme VIRTUÁLNÍ čas
        var result: TagRegistration<*>? = null

        val virtualTimeBefore = testScheduler.currentTime // nebo jen currentTime
        result = registry.resolveTag<MyDummyTag>("another_missing_tag")
        val virtualTimeAfter = testScheduler.currentTime

        // Assert:
        assertNull(result)
        // Virtuální čas se nesměl posunout ani o milisekundu (funkce nesuspendovala)
        assertEquals(virtualTimeBefore, virtualTimeAfter)
    }
}

// Dummy třída pro testování
class MyDummyTag : Tag