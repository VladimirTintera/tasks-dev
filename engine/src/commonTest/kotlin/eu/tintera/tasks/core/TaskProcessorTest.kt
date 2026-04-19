package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.fakes.*
import eu.tintera.tasks.core.preconditions.NetworkStateTaskPrecondition
import eu.tintera.tasks.core.preconditions.TaskPreconditionController
import eu.tintera.tasks.core.serialization.UnitTaskDataSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class TaskProcessorTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when iOS wake lock expires, task is cancelled and token released but state remains running`() = runTest {
        // 1. Příprava závislostí
        val fakeWakeLock = FakeExecutionContextProvider()

        // Zde si vytvoříš jednoduché fakes nebo mocky pro svá rozhraní
        val fakeRepository = FakeRepository() // Musí vracet flow stavů a rodičů
        val fakeNetworkState = FakeNetworkState()

        // Evaluator, který trvá nesmyslně dlouho (např. 10 minut),
        // takže ho expiration handler určitě přeruší
        val fakeEvaluator = TaskEvaluatorImpl(
            taskRegistry = fakeTaskRegistry(),
            repository = fakeRepository,
            taskMigrator = TaskMigrator(fakeRepository),
            taskScopeFactory = TaskScopeFactory(fakeRepository),
            applicationScope = ApplicationScope(SupervisorJob()),
            dispatchers = dispatchers()
        )

        val processor = TaskProcessorImpl(
            repository = fakeRepository,
            taskEvaluator = fakeEvaluator,
            executionContextProvider = fakeWakeLock,
            preconditionController = TaskPreconditionController(emptyList()),
            taskResultProcessor = TaskResultProcessorImpl(fakeRepository)
        )

        // Vytvoříme testovací task, který vyžaduje iOS KeepAlive
        val task = Task(
            id = Uuid.random(),
            state = State.Enqueued,
            identifier = "fakeTask",
            uniqueName = "fakeTask",
            runAttemptCount = 0,
            initialDelay = Duration.ZERO,
            processTime = Clock.System.now(),
            inputData = null,
            outputData = null,
            networkRequired = false,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = null,
            backoffCriteria = BackoffCriteria.DEFAULT,
            progressData = null,
            retentionDelay = 24.hours,
            requiresDeviceIdle = false,
            version = 1
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        assertEquals(fakeRepository.taskState(task.id), State.Enqueued)

        // 2. Spuštění processoru v samostatné coroutině (aby nám neblokoval test)
        val processorJob = launch {
            processor.run(task)
        }

        // Posuneme virtuální čas o kousek, aby task stihl začít pracovat (např. 1 vteřinu)
        advanceTimeBy(1000.milliseconds)

        // Ověříme, že Task začal běžet a token se ještě neuvolnil
        assertEquals(fakeRepository.taskState(task.id), State.Running, "task must be running")
        assertFalse(fakeWakeLock.token.isExpired.value, "token is not expired")

        // 3. AKCE: Nasimulujeme, že iOS volá expiration handler!
        fakeWakeLock.cancel()

        // Necháme všechny coroutiny, ať zpracují zrušení
        advanceUntilIdle()

        // 4. ASSERTION (Ověření správného chování)

        // A) Processor job by měl být korektně dokončený (nevyhodil crash do aplikace)
        assertTrue(processorJob.isCompleted, "Job is completed")
        assertFalse(processorJob.isCancelled, "Job is not cancelled") // Job doběhl normálně

        // B) Wakelock Token MUSÍ být uvolněn přes finally/use blok!
        assertTrue(fakeWakeLock.token.isExpired.value)

        // C) Task nesmí být označen jako Failed nebo Success, musí zůstat viset
        // (protože mainJob byl zrušen a přeskočil se zápis do DB).
        assertEquals(fakeRepository.taskState(task.id), State.Running)
    }

    @Test
    fun `when task finishes successfully, state is updated to Succeeded`() = runTest {
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
    fun `when task requests retry, it is rescheduled with backoff`() = runTest {
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
    fun `when parent task failed, child task fails too`() = runTest {
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
    fun `when network is required but not connected, task waits`() = runTest {
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
    fun `when network is required but not connected, task waits, when network disconnects, task cancels`() = runTest {
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
    fun `when task has initial delay, it waits before execution`() = runTest {
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
    fun `when task throws exception, it is marked as failed`() = runTest {
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


        // Vytvoříme task, který má běžet až za 1 hodinu
        val task = createTask(
            identifier = "delayedTask",
            initialDelay = 1.hours
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Spustíme procesor (coroutina se uspí)
        val job = launch {
            processor.run(task)
        }

        // Posuneme čas jen o 30 minut
        advanceTimeBy(30.minutes)
        runCurrent()

        // ASSERT: Zatím nesměl být vyžádán žádný iOS zámek!
        assertEquals(0, fakeProvider.acquireCount, "Během čekání se nesmí zamykat OS")

        // Posuneme čas za hranici (task by měl začít běžet)
        advanceTimeBy(31.minutes)

        assertEquals(State.Running, fakeRepository.taskState(task.id))
        runCurrent()

        // ASSERT: Až teď se musel zámek vyžádat
        assertEquals(1, fakeProvider.acquireCount, "Před samotnou exekucí se musí OS zamknout")

        job.cancelAndJoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when task is cancelled in DB during execution, processor stops it`() = runTest {

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


        // Vytvoříme task, který má běžet až za 1 hodinu
        val task = createTask(
            identifier = "cancelledOutside",
        )

        fakeRepository.insert(task, emptySet(), emptySet())

        // Spustíme procesor (coroutina se uspí)
        val job = launch {
            processor.run(task)
        }

        // Posuneme čas o 1 vteřinu (Task přejde do stavu Running)
        advanceTimeBy(1.seconds)
        runCurrent()

        assertEquals(State.Running, fakeRepository.taskState(task.id), "Task is running")
        // AKCE (Sabotáž zvenčí): UI vlákno změní stav tasku v DB!
        fakeRepository.updateState(task.id, State.Cancelled, State.entries.toSet())

        assertEquals(State.Cancelled, fakeRepository.taskState(task.id), "Task is cancelled")

        // Necháme Coroutine Dispatcher zpracovat tu změnu ve Flow
        runCurrent()

        // ASSERT: job.isCancelled musí být true!
        // Náš DB Watcher zjistil, že state.terminal() je true, a zavolal mainJob.cancelAndJoin()
        assertTrue(job.isCompleted, "Processor musí zrušit běh, pokud je task zrušen v DB")
    }
}
