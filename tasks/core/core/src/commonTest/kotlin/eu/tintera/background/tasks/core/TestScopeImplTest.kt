package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.core.data.TaskScopeRepository
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class TaskScopeImplTest {

    // Fake dependencies for the test.
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
            scope = coroutineScope, // the test scope
            repository = fakeRepository,
            progressSerializer = FakeIntSerializer(), // custom fake
            tags = emptySet(),
            saveDispatcher = testDispatcher, // gives the test control over IO timing
            log = CompositeTasksLogger(emptyList())
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `setProgress throttles database saves to 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope) // backgroundScope cancels itself when the test ends

        // Act: fire three updates at millisecond zero.
        scope.setProgress(10)
        scope.setProgress(20)
        scope.setProgress(30)

        // Assert: nothing written yet — waiting for the 300ms tick.
        assertEquals(0, fakeRepository.saveCount)

        // Act: advance by 300 milliseconds.
        advanceTimeBy(300.milliseconds)
        runCurrent() // drain the coroutines queued on the dispatcher

        // Assert: written exactly ONCE, with the latest value (30).
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(30, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `exception in trySave does not crash scope and allows future updates`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        // Simulate a broken database.
        fakeRepository.shouldThrowException = true

        // Act: first attempt, which fails.
        scope.setProgress(50)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: the DB was tried and failed, but the coroutine must stay alive.
        assertEquals(1, fakeRepository.saveCount)

        // The database recovers.
        fakeRepository.shouldThrowException = false

        // Act: another progress update — must go through, the scope must not be dead.
        scope.setProgress(100)
        advanceTimeBy(300.milliseconds)
        runCurrent()

        // Assert: the second write succeeded.
        assertEquals(2, fakeRepository.saveCount)
        assertEquals(100, fakeRepository.lastSavedProgress)
    }

    @Test
    fun `flushProgress saves immediately without waiting for 300ms`() = runTest(testDispatcher) {
        createScope(backgroundScope)

        scope.setProgress(99)

        // Assert: nothing stored before the flush.
        assertEquals(0, fakeRepository.saveCount)

        // Cancel the background collector job so it does not save concurrently
        scope.close()

        // Act: flush manually.
        val job = launch {
            scope.flushProgress()
        }
        runCurrent()
        job.join()

        // Assert: written immediately — no time was advanced.
        assertEquals(1, fakeRepository.saveCount)
        assertEquals(99, fakeRepository.lastSavedProgress)
    }

    // --- Fakes that keep the test isolated ---

    class FakeRepository : TaskScopeRepository { // only what the test needs
        var saveCount = 0
        var lastSavedProgress: Int? = null
        var shouldThrowException = false

        override suspend fun updateProgressData(id: Uuid, progressData: ByteArray?) {
            saveCount++
            if (shouldThrowException) throw Exception("Fake DB locked!")
            // Production stores a ByteArray; the test unwraps the original Int for simplicity.
            lastSavedProgress = progressData?.decodeToString()?.toIntOrNull()
        }
    }

    class FakeIntSerializer : Serializer<Int> {
        override fun encodeToBytes(value: Int): ByteArray = value.toString().encodeToByteArray()
        override fun decodeFromBytes(bytes: ByteArray): Int = bytes.decodeToString().toInt()
    }
}