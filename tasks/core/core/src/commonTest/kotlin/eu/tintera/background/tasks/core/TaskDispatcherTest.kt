package eu.tintera.background.tasks.core

/*
import eu.tintera.background.tasks.Data
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.fakes.FakeRepository
import eu.tintera.background.tasks.core.fakes.FakeTaskProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid


class TaskDispatcherTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execution key changes - old job is cancelled and new job starts`() = runTest {

        // 1. Setup
        val fakeRepo = FakeRepository()
        val fakeProcessor = FakeTaskProcessor()

        // Předáme náš testovací dispatcher!
        TaskDispatcher(
            taskProcessor = fakeProcessor,
            repository = fakeRepo,
            scope = ApplicationScope(SupervisorJob()),
            dispatchers = dispatchers(),
            activeTaskTracker = ActiveTaskTrackerImpl()
        )

        val taskId = Uuid.random()
        val initialTime = Clock.System.now()

        // Vytvoříme první verzi tasku
        val taskV1 = createTask(
            identifier = "cancelledOutside",
            id = taskId,
            processTime = initialTime,
            runAttemptCount = 0
        )

        val keyV1 = ExecutionKey(taskV1.id, taskV1.processTime)

        // 2. Akce: Vyemitujeme task z DB
        fakeRepo.insert(taskV1, emptySet(), emptySet())

        // runCurrent() "popostrčí" všechny čekající coroutiny na testovacím dispatcheru,
        // aby okamžitě zpracovaly emisi z Flow
        runCurrent()

        // 3. Ověření: TaskV1 musí běžet
        assertTrue(fakeProcessor.currentlyRunningKeys.contains(keyV1), "Starý job měl běžet!")

        // 4. Akce: Task selhal, DB ho zaktualizovala (změna retries a času)

        val keyV2 = ExecutionKey(
            id = taskId,
            processTime = initialTime + 1.minutes
        )

        fakeRepo.updateNextRun(
            id = taskId,
            processTime = keyV2.processTime,
            state = State.Enqueued,
            runAttemptCount = null,
            progressData = Data.EMPTY
        )

        runCurrent()

        // 5. Ověření: Původní job byl zrušen a nahrazen novým
        assertFalse(fakeProcessor.currentlyRunningKeys.contains(keyV1), "Old job should by cancelled!")
        assertTrue(fakeProcessor.currentlyRunningKeys.contains(keyV2), "New job should be running!")
    }
}*/