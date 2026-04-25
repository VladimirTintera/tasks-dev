package eu.tintera.tasks.core

import eu.tintera.tasks.core.data.TaskScopeRepository
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class TaskScopeImplTest {

    // 1. Vytvoříme si falešné závislosti pro test
    private lateinit var fakeRepository: FakeRepository
    private lateinit var scope: TaskScopeImpl<String, Int> // Input=String, Progress=Int
    private lateinit var testDispatcher: TestDispatcher

    @BeforeTest
    fun setup() {
        fakeRepository = FakeRepository()
        testDispatcher = StandardTestDispatcher()
    }

    private fun createScope(coroutineScope: CoroutineScope) {
        scope = TaskScopeImpl(
            taskId = Uuid.random(),
            data = "TestInput",
            retryCount = 0,
            parents = emptyList(),
            onForegroundInfoProvided = { true },
            scope = coroutineScope, // Vložíme testovací scope
            repository = fakeRepository,
            progressSerializer = FakeIntSerializer(), // Vlastní mock
            tags = emptySet(),
            typedTags = emptySet(),
            saveDispatcher = testDispatcher // Ovládáme časování IO!
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setProgress throttles database saves to 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope) // backgroundScope se sám na konci testu zruší

        // Act: Napálíme tam tři updaty v nulté milisekundě
        scope.setProgress(10)
        scope.setProgress(20)
        scope.setProgress(30)

        // Assert: Zatím se do DB nezapsalo nic (čeká se na 300ms tick)
        assertEquals(0, fakeRepository.saveCount)

        // Act: Posuneme čas o 300 milisekund
        advanceTimeBy(300.milliseconds)
        runCurrent() // Ujistíme se, že se vykonají coroutiny zapsané v dispatcher frontě

        // Assert: Zapsalo se to pouze JEDNOU a s poslední hodnotou (30)
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(30, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `exception in trySave does not crash scope and allows future updates`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        // Nasimulujeme rozbitou databázi
        fakeRepository.shouldThrowException = true

        // Act: První pokus, který spadne
        scope.setProgress(50)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: DB to zkusila, ale spadla. Coroutina však musí dál žít!
        assertEquals(1, fakeRepository.saveCount)

        // Databáze se "opravila"
        fakeRepository.shouldThrowException = false

        // Act: Další progress (musí projít, scope nesmí být mrtvý)
        scope.setProgress(100)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: Druhý zápis prošel úspěšně!
        assertEquals(2, fakeRepository.saveCount)
        assertEquals(100, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `flushProgress saves immediately without waiting for 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        scope.setProgress(99)

        // Assert: Před flushem je uloženo 0 krát
        assertEquals(0, fakeRepository.saveCount)

        // Act: Voláme manuální flush
        scope.flushProgress()
        runCurrent()

        // Assert: Bylo zapsáno okamžitě (čas se neposouval!)
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(99, fakeRepository.lastSavedProgress)
    }

    // --- Pomocné Mock třídy pro izolaci testu ---

    class FakeRepository : TaskScopeRepository { // Tady implementuj jen to nejnutnější
        var saveCount = 0
        var lastSavedProgress: Int? = null
        var shouldThrowException = false

        override suspend fun updateProgressData(id: Uuid, progressData: ByteArray?) {
            saveCount++
            if (shouldThrowException) throw Exception("Fake DB locked!")
            // V reálu ukládáš ByteArray, tady si pro jednoduchost testu
            // vytáhneme tu původní Int hodnotu (pokud to Serializer umožní nasimulovat)
            lastSavedProgress = progressData?.decodeToString()?.toIntOrNull()
        }
    }

    class FakeIntSerializer : Serializer<Int> {
        override fun encodeToBytes(value: Int): ByteArray = value.toString().encodeToByteArray()
        override fun decodeFromBytes(bytes: ByteArray): Int = bytes.decodeToString().toInt()
    }
}