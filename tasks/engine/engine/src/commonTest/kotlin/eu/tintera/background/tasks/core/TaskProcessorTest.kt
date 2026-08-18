package eu.tintera.background.tasks.core

import eu.tintera.background.guard.ExecutionContextProvider
import eu.tintera.background.tasks.BackoffCriteria
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.data.Task
import eu.tintera.background.tasks.core.fakes.*
import eu.tintera.background.tasks.core.migrations.TaskMigrator
import eu.tintera.background.tasks.core.constraints.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class FakeTaskResultHandler(
    private val repository: FakeRepository
) : TaskResultHandler {
    override suspend fun handleResult(result: TaskEvaluationResult) {
        val now = Clock.System.now()
        val runningStates = setOf(State.Running)
        when (result) {
            is TaskEvaluationResult.Failed -> {
                val duration = result.repeatInterval
                if (duration != null) repository.scheduleNextFromBeginning(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + duration,
                    allowedSourceStates = runningStates
                )
                else repository.failTask(
                    id = result.id,
                    state = State.Failed,
                    finishedAt = now,
                    allowedSourceStates = runningStates
                )
            }
            is TaskEvaluationResult.Success -> {
                val duration = result.repeatInterval
                if (duration != null) repository.scheduleNextFromBeginning(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + duration,
                    allowedSourceStates = runningStates
                )
                else repository.successTask(
                    id = result.id,
                    state = State.Succeeded,
                    finishedAt = now,
                    outputData = ByteArray(0),
                    allowedSourceStates = runningStates
                )
            }
            is TaskEvaluationResult.Retry -> {
                val backoff = (result.backoffCriteria ?: defaultBackoffCriteria).calculate(result.retryCount)
                repository.scheduleNext(
                    id = result.id,
                    state = State.Enqueued,
                    processTime = now + backoff,
                    allowedSourceStates = runningStates
                )
            }
        }
    }
}

class TaskProcessorTest {

    private fun TestScope.createTaskEvaluator(
        fakeRepository: FakeRepository,
        registryResolver: RegistryResolver = FakeRegistryResolver()
    ): TaskEvaluator {
        return TaskEvaluatorImpl(
            registryResolver = registryResolver,
            taskMigrator = TaskMigrator(),
            taskScopeFactory = TaskScopeFactory(fakeRepository, CompositeTasksLogger(emptyList())),
            applicationScope = ApplicationScope(SupervisorJob()),
            dispatchers = dispatchers(),
            tagMapper = TagMapper(registryResolver),
            repository = fakeRepository,
            taskResultHandler = FakeTaskResultHandler(fakeRepository),
            log = CompositeTasksLogger(emptyList())
        )
    }

    private fun createTaskProcessor(
        repository: FakeRepository,
        taskEvaluator: TaskEvaluator,
        executionContextProvider: ExecutionContextProvider = FakeExecutionContextProvider(),
        taskLifecycleObserver: CompositeTaskLifecycleObserver = CompositeTaskLifecycleObserver(emptyList()),
        preconditionController: ConstraintController = ConstraintController(emptyList())
    ): TaskProcessorImpl {
        return TaskProcessorImpl(
            taskEvaluator = taskEvaluator,
            executionContextProvider = executionContextProvider,
            preconditionController = preconditionController,
            taskLifecycleObserver = taskLifecycleObserver,
            repository = repository,
            clock = Clock.System
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when iOS wake lock expires - task is cancelled and token released but state remains running`() = runTest {
        // 1. Set up dependencies.
        val fakeWakeLock = FakeExecutionContextProvider()

        // Simple fakes for the interfaces under test.
        val fakeRepository = FakeRepository() // must return flows of states and parents
        val fakeNetworkState = FakeNetworkState()

        val fakeEvaluator = createTaskEvaluator(fakeRepository, fakeTaskRegistry())

        val processor = createTaskProcessor(fakeRepository, fakeEvaluator, fakeWakeLock)

        // A task that requires the iOS keep-alive.
        val task = Task(
            id = Uuid.random(),
            state = State.Enqueued,
            identifier = "fakeTask",
            uniqueName = "fakeTask",
            runAttemptCount = 0,
            initialDelay = Duration.ZERO,
            processTime = Clock.System.now(),
            inputData = ByteArray(0),
            outputData = null,
            networkRequired = false,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = null,
            backoffCriteria = defaultBackoffCriteria,
            progressData = null,
            retentionDelay = 24.hours,
            requiresDeviceIdle = false,
            version = 1
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        assertEquals(State.Enqueued, fakeRepository.taskState(task.id))

        // 2. Run the processor in its own coroutine so it does not block the test.
        val processorJob = launch {
            processor.run(task.id)
        }

        // Advance virtual time a little so the task gets going (1 second).
        advanceTimeBy(1000.milliseconds)

        // The task is running and the token has not been released yet.
        assertEquals(State.Running, fakeRepository.taskState(task.id), "task must be running")
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
        assertEquals(State.Running, fakeRepository.taskState(task.id))
    }

    @Test
    fun `when task finishes successfully - state is updated to Succeeded`() = runTest {
        val fakeRepository = FakeRepository()
        val registry = FakeRegistryResolver().apply {
            register("successTask") { TaskResult.success(Unit) }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(fakeRepository, fakeEvaluator)

        val task = createTask(identifier = "successTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task.id)

        assertEquals(State.Succeeded, fakeRepository.taskState(task.id), "Task should be succeeded")
    }

    @Test
    fun `when task requests retry - it is rescheduled with backoff`() = runTest {
        val fakeRepository = FakeRepository()
        val registry = FakeRegistryResolver().apply {
            register("retryTask") { TaskResult.Retry }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(fakeRepository, fakeEvaluator)

        val task = createTask(identifier = "retryTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task.id)

        val updatedTask = fakeRepository.task(task.id)!!
        assertEquals(State.Enqueued, updatedTask.state, "Task should be enqueued")
        assertEquals(1, updatedTask.runAttemptCount, "Task should have 1 retry")
        val updatedProcessTime = updatedTask.processTime ?: error("processTime should not be null")
        val taskProcessTime = task.processTime ?: error("task.processTime should not be null")
        assertTrue(updatedProcessTime > taskProcessTime, "Task should have been rescheduled")
    }

    @Test
    fun `when parent task failed - child task fails too`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeEvaluator = createTaskEvaluator(fakeRepository, FakeRegistryResolver())
        val processor = createTaskProcessor(fakeRepository, fakeEvaluator)

        val parentTask = createTask(identifier = "parent", state = State.Failed)
        val childTask = createTask(identifier = "child")

        fakeRepository.insert(parentTask, emptySet(), emptySet())
        fakeRepository.insert(childTask, emptySet(), setOf(parentTask.id))

        processor.run(childTask.id)

        assertEquals(State.Failed, fakeRepository.taskState(childTask.id))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when network is required but not connected - task waits`() = runTest {
        val fakeRepository = FakeRepository()
        val fakeNetworkState = FakeNetworkState(NetworkState.State.Disconnected)

        val registry = FakeRegistryResolver().apply {
            register("networkTask") { TaskResult.success(Unit) }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            preconditionController = ConstraintController(
                listOf(
                    NetworkStateConstraint(fakeNetworkState)
                )
            )
        )

        val task = createTask(identifier = "networkTask", networkRequired = true)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task.id)
        }

        advanceUntilIdle()

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

        val registry = FakeRegistryResolver().apply {
            register("networkTask") {
                delay(10.seconds)
                TaskResult.success(Unit)
            }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            preconditionController = ConstraintController(
                listOf(
                    NetworkStateConstraint(fakeNetworkState)
                )
            )
        )

        val task = createTask(identifier = "networkTask", networkRequired = true)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task.id)
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
        val registry = FakeRegistryResolver().apply {
            register("delayedTask") { TaskResult.success(Unit) }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            preconditionController = ConstraintController(
                listOf(
                    InitialDelayConstraint()
                )
            )
        )

        val delayDuration = 10.seconds
        val task = createTask(identifier = "delayedTask", initialDelay = delayDuration)
        fakeRepository.insert(task, emptySet(), emptySet())

        val job = launch {
            processor.run(task.id)
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
        val registry = FakeRegistryResolver().apply {
            register("exceptionTask") { throw RuntimeException("Crash!") }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)
        val processor = createTaskProcessor(fakeRepository, fakeEvaluator)

        val task = createTask(identifier = "exceptionTask")
        fakeRepository.insert(task, emptySet(), emptySet())

        processor.run(task.id)

        assertEquals(State.Failed, fakeRepository.taskState(task.id))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `task waiting for processTime does not hold system wake lock`() = runTest {
        val fakeRepository = FakeRepository()

        val registry = FakeRegistryResolver().apply {
            register("delayedTask") {
                delay(1.hours)
                TaskResult.success(Unit)
            }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)

        val fakeProvider = FakeTokenProvider()
        val processor = createTaskProcessor(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = executionContextProvider(fakeProvider),
            preconditionController = ConstraintController(
                listOf(
                    ProcessTimePrecondition()
                )
            )
        )

        // A task scheduled to run in an hour.
        val task = createTask(
            identifier = "delayedTask",
            initialDelay = 1.hours
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Start the processor; the coroutine goes to sleep.
        val job = launch {
            processor.run(task.id)
        }

        // Advance only 30 minutes.
        advanceTimeBy(30.minutes)
        runCurrent()

        // ASSERT: no iOS lock may have been requested yet.
        assertEquals(0, fakeProvider.acquireCount, "The OS must not be locked while waiting")

        // Advance past the threshold; the task should start.
        advanceTimeBy(31.minutes)
        runCurrent()

        assertEquals(State.Running, fakeRepository.taskState(task.id))

        // ASSERT: only now must the lock have been requested.
        assertEquals(1, fakeProvider.acquireCount, "The OS must be locked before execution")

        job.cancelAndJoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when task is cancelled in DB during execution - processor stops it`() = runTest {
        val fakeRepository = FakeRepository()

        val registry = FakeRegistryResolver().apply {
            register("cancelledOutside") {
                delay(1.hours)
                TaskResult.success(Unit)
            }
        }
        val fakeEvaluator = createTaskEvaluator(fakeRepository, registry)

        val fakeProvider = FakeTokenProvider()
        val processor = createTaskProcessor(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = executionContextProvider(fakeProvider)
        )

        // A task scheduled to run in an hour.
        val task = createTask(
            identifier = "cancelledOutside",
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Start the processor; the coroutine goes to sleep.
        val job = launch {
            processor.run(task.id)
        }

        // Advance 1 second; the task moves to Running.
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(State.Running, fakeRepository.taskState(task.id), "Task is running")
        // ACT (external interference): the UI thread changes the task state in the DB.
        fakeRepository.updateState(
            id = task.id,
            state = State.Cancelled,
            allowedSourceStates = State.entries.toSet(),
            resetProcessTime = false,
            runAttemptCount = null
        )

        assertEquals(State.Cancelled, fakeRepository.taskState(task.id), "Task is cancelled")

        // Let the dispatcher process the change coming through the flow.
        runCurrent()

        // ASSERT: job.isCancelled must be true!
        // The DB watcher saw state.terminal() and called mainJob.cancelAndJoin().
        assertTrue(job.isCompleted, "The processor must stop when the task is cancelled in the DB")
    }
}
