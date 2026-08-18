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

        // Hand in the test dispatcher.
        TaskDispatcher(
            taskProcessor = fakeProcessor,
            repository = fakeRepo,
            scope = ApplicationScope(SupervisorJob()),
            dispatchers = dispatchers(),
            activeTaskTracker = ActiveTaskTrackerImpl()
        )

        val taskId = Uuid.random()
        val initialTime = Clock.System.now()

        // First version of the task.
        val taskV1 = createTask(
            identifier = "cancelledOutside",
            id = taskId,
            processTime = initialTime,
            runAttemptCount = 0
        )

        val keyV1 = ExecutionKey(taskV1.id, taskV1.processTime)

        // 2. Akce: Vyemitujeme task z DB
        fakeRepo.insert(taskV1, emptySet(), emptySet())

        // runCurrent() nudges every pending coroutine on the test dispatcher so the flow emission
        // is processed right away.
        runCurrent()

        // 3. Assert: TaskV1 must be running.
        assertTrue(fakeProcessor.currentlyRunningKeys.contains(keyV1), "The old job should have been running")

        // 4. Act: the task failed and the DB updated it (retry count and time changed).

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

        // 5. Assert: the original job was cancelled and replaced.
        assertFalse(fakeProcessor.currentlyRunningKeys.contains(keyV1), "Old job should by cancelled!")
        assertTrue(fakeProcessor.currentlyRunningKeys.contains(keyV2), "New job should be running!")
    }
}*/