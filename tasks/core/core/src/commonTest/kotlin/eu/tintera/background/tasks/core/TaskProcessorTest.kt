package eu.tintera.background.tasks.core

/*
import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.Data
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.data.Task
import eu.tintera.background.tasks.core.fakes.*
import eu.tintera.background.tasks.core.preconditions.NetworkStateTaskPrecondition
import eu.tintera.background.tasks.core.preconditions.TaskPreconditionController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class TaskProcessorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when iOS wake lock expires - task is cancelled and token released but state remains running`() = runTest {
        // 1. Set up dependencies.
        val fakeWakeLock = FakeExecutionContextProvider()

        // Simple fakes for the interfaces under test.
        val fakeRepository = FakeRepository() // must return flows of states and parents
        val fakeNetworkState = FakeNetworkState()

        // An evaluator that takes absurdly long (10 minutes), so the expiration handler
        // definitely interrupts it.
        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("fakeTask") {
                    {
                        delay(10.minutes) // 10 minutes of virtual time
                        TaskResult.success()
                    }
                }
            }
        )

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = fakeWakeLock,
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        // A task that requires the iOS keep-alive.
        val task = Task(
            id = Uuid.random(),
            state = State.Enqueued,
            identifier = "fakeTask",
            uniqueName = "fakeTask",
            runAttemptCount = 0,
            initialDelay = Duration.ZERO,
            processTime = Clock.System.now(),
            inputData = Data.EMPTY,
            outputData = Data.EMPTY,
            networkRequired = false,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = null,
            backoffCriteria = BackoffCriteria.DEFAULT,
            progressData = null,
            retentionDelay = 24.hours,
            requiresDeviceIdle = false
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        assertEquals(fakeRepository.taskState(task.id), State.Enqueued)

        // 2. Run the processor in its own coroutine so it does not block the test.
        val processorJob = launch {
            processor.run(task)
        }

        // Advance virtual time a little so the task gets going (1 second).
        advanceTimeBy(1000)

        // The task is running and the token has not been released yet.
        assertEquals(fakeRepository.taskState(task.id), State.Running, "task must be running")
        assertFalse(fakeWakeLock.token.isExpired.value, "token is not expired")

        // 3. ACT: simulate iOS invoking the expiration handler.
        fakeWakeLock.cancel()

        // Let every coroutine process the cancellation.
        advanceUntilIdle()

        // 4. ASSERT.

        // A) The processor job finished cleanly and did not crash the application.
        assertTrue(processorJob.isCompleted, "Job is completed")
        assertFalse(processorJob.isCancelled, "Job is not cancelled") // finished normally

        // B) The wake-lock token MUST be released through the finally/use block.
        assertTrue(fakeWakeLock.token.isExpired.value)

        // C) The task must be left pending rather than marked Failed or Success — mainJob was
        //    cancelled, so the DB write was skipped.
        assertEquals(fakeRepository.taskState(task.id), State.Running)
    }

    @Test
    fun `when task finishes successfully - state is updated to Succeeded`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("successTask") { { TaskResult.success() } }
            }
        )
        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        val task = createTask(identifier = "successTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task)

        assertEquals(State.Succeeded, fakeRepository.taskState(task.id), "Task should be succeeded")
    }

    @Test
    fun `when task requests retry - it is rescheduled with backoff`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("retryTask") { { TaskResult.Retry } }
            }
        )
        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        val task = createTask(identifier = "retryTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task)

        val updatedTask = fakeRepository.task(task.id).first()!!
        assertEquals(State.Enqueued, updatedTask.state, "Task should be enqueued")
        assertEquals(1, updatedTask.runAttemptCount, "Task should have 1 retry")
        assertTrue(updatedTask.processTime > task.processTime, "Task should have been rescheduled")
    }

    @Test
    fun `when parent task failed - child task fails too`() = runTest {
        val fakeRepository = FakeRepository()
        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = TaskEvaluator(TaskRegistry()),
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        val parentTask = createTask(identifier = "parent", state = State.Failed)
        val childTask = createTask(identifier = "child")

        fakeRepository.insert(parentTask, emptySet(), emptySet())
        fakeRepository.insert(childTask, emptySet(), setOf(parentTask.id))

        processor.run(childTask)

        assertEquals(State.Failed, fakeRepository.taskState(childTask.id))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when network is required but not connected - task waits`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeNetworkState = FakeNetworkState(NetworkState.State.Disconnected)

        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("networkTask") {
                    {
                        TaskResult.success()
                    }
                }
            }
        )

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(
                listOf(
                    NetworkStateTaskPrecondition(fakeNetworkState)
                )
            )
        )

        val task = createTask(identifier = "networkTask", networkRequired = true)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task)
        }

        advanceUntilIdle()

        //advanceUntilIdle()
        // Task should be waiting for network, so it's not finished yet
        // Note: In the current implementation, it might be Enqueued or Running depending on where it pauses.
        // Based on TaskProcessor code:
        // updateState(task.id, TaskState.Enqueued) happens before network check.
        // Then it waits for network.
        // So state should be Enqueued.
        assertEquals(State.Enqueued, fakeRepository.taskState(task.id), "Task should be waiting for network")
        assertFalse(job.isCompleted, "Job should not be completed")

        // Connect network
        fakeNetworkState.networkState.value = NetworkState.State.Connected
        advanceUntilIdle()

        assertTrue(job.isCompleted, "Job should be completed")
        assertEquals(State.Succeeded, fakeRepository.taskState(task.id), "Task should be succeeded")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when network is required but not connected - task waits - when network disconnects - task cancels`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeNetworkState = FakeNetworkState(NetworkState.State.Disconnected)

        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("networkTask") {
                    {
                        delay(10.seconds)
                        TaskResult.success()
                    }
                }
            }
        )

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(
                listOf(
                    NetworkStateTaskPrecondition(fakeNetworkState)
                )
            )
        )

        val task = createTask(identifier = "networkTask", networkRequired = true)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task)
        }

        advanceUntilIdle()

        assertEquals(State.Enqueued, fakeRepository.taskState(task.id), "Task should be waiting for network")
        assertFalse(job.isCompleted, "Job should not be completed")

        // Connect network
        fakeNetworkState.networkState.value = NetworkState.State.Connected
        advanceTimeBy(5.seconds)

        assertEquals(State.Running, fakeRepository.taskState(task.id), "Task should be running")
        assertFalse(job.isCompleted, "Job should not be completed")

        fakeNetworkState.networkState.value = NetworkState.State.Disconnected
        advanceTimeBy(1.seconds)

        assertTrue(job.isCompleted, "Job should be completed")
        assertEquals(State.Enqueued, fakeRepository.taskState(task.id), "Task should be enqueued")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when task has initial delay - it waits before execution`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("delayedTask") { { TaskResult.success() } }
            }
        )
        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        val delayDuration = 10.seconds
        val task = createTask(identifier = "delayedTask", initialDelay = delayDuration)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task)
        }

        // Advance time less than delay
        advanceTimeBy(5000)
        // Task should still be enqueued (waiting)
        assertEquals(State.Enqueued, fakeRepository.taskState(task.id))
        assertFalse(job.isCompleted)

        // Advance time past delay
        advanceTimeBy(6000)
        advanceUntilIdle()

        assertTrue(job.isCompleted)
        assertEquals(State.Succeeded, fakeRepository.taskState(task.id))
    }

    @Test
    fun `when task throws exception - it is marked as failed`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("exceptionTask") { { throw RuntimeException("Crash!") } }
            }
        )
        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = FakeExecutionContextProvider(),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )

        val task = createTask(identifier = "exceptionTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task)

        assertEquals(State.Failed, fakeRepository.taskState(task.id))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `task waiting for processTime does not hold system wake lock`() = runTest {
        val fakeRepository = FakeRepository()

        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("delayedTask") {
                    {
                        delay(1.hours)
                        TaskResult.success()
                    }
                }
            }
        )

        val fakeProvider = FakeTokenProvider()

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = executionContextProvider(fakeProvider),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )


        // A task scheduled to run in an hour.
        val task = createTask(
            identifier = "delayedTask",
            initialDelay = 1.hours
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Start the processor; the coroutine goes to sleep.
        val job = launch {
            processor.run(task)
        }

        // Advance only 30 minutes.
        advanceTimeBy(30.minutes)
        runCurrent()

        // ASSERT: no iOS lock may have been requested yet.
        assertEquals(0, fakeProvider.acquireCount, "The OS must not be locked while waiting")

        // Advance past the threshold; the task should start.
        advanceTimeBy(31.minutes)

        assertEquals(State.Running, fakeRepository.taskState(task.id))
        runCurrent()

        // ASSERT: only now must the lock have been requested.
        assertEquals(1, fakeProvider.acquireCount, "The OS must be locked before execution")

        job.cancelAndJoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when task is cancelled in DB during execution - processor stops it`() = runTest {

        val fakeRepository = FakeRepository()

        val fakeEvaluator = TaskEvaluator(
            taskRegistry = TaskRegistry().apply {
                register("cancelledOutside") {
                    {
                        delay(1.hours)
                        TaskResult.success()
                    }
                }
            }
        )

        val fakeProvider = FakeTokenProvider()

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = executionContextProvider(fakeProvider),
            taskScopeFactory = FakeTaskScopeFactory(),
            preconditionController = TaskPreconditionController(emptyList())
        )


        // A task scheduled to run in an hour.
        val task = createTask(
            identifier = "cancelledOutside",
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Start the processor; the coroutine goes to sleep.
        val job = launch {
            processor.run(task)
        }

        // Advance 1 second; the task moves to Running.
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(State.Running, fakeRepository.taskState(task.id), "Task is running")
        // ACT (external interference): the UI thread changes the task state in the DB.
        fakeRepository.updateState(task.id, State.Cancelled, State.entries.toSet())

        assertEquals(State.Cancelled, fakeRepository.taskState(task.id), "Task is cancelled")

        // Let the dispatcher process the change coming through the flow.
        runCurrent()

        // ASSERT: the DB watcher saw state.terminal() and called mainJob.cancelAndJoin().
        assertTrue(job.isCompleted, "The processor must stop when the task is cancelled in the DB")
    }
}
*/