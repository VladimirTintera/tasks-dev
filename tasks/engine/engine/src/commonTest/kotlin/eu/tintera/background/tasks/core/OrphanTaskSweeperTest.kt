package eu.tintera.background.tasks.core

import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.core.fakes.FakeAppStateObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class OrphanTaskSweeperTest {

    private class FakeOrphanTaskSweeperRepository : OrphanTaskSweeperRepository {
        val resetCalls = mutableListOf<ResetCall>()

        data class ResetCall(
            val from: State,
            val to: State,
            val excludedIds: Set<Uuid>
        )

        override suspend fun resetState(from: State, to: State, excludedIds: Set<Uuid>) {
            resetCalls.add(ResetCall(from, to, excludedIds))
        }
    }

    private class FakeActiveTaskTracker(private val activeIds: Set<Uuid>) : ActiveTaskTracker {
        override fun getActiveIds(): Set<Uuid> = activeIds
    }

    @Test
    fun `when sweeper is initialized, a cold start sweep is performed excluding active tasks`() = runTest {
        val repository = FakeOrphanTaskSweeperRepository()
        val activeIds = setOf(Uuid.random(), Uuid.random())
        val activeTaskTracker = FakeActiveTaskTracker(activeIds)
        val isBackground = MutableStateFlow(true)
        val appStateObserver = FakeAppStateObserver(isBackground)

        val sweeperJob = Job()
        // Initialize sweeper
        OrphanTaskSweeper(
            repository = repository,
            scope = ApplicationScope(coroutineContext + sweeperJob),
            dispatchers = dispatchers(),
            activeTaskTracker = activeTaskTracker,
            appStateObserver = appStateObserver
        )

        runCurrent()

        assertEquals(1, repository.resetCalls.size)
        val call = repository.resetCalls.first()
        assertEquals(State.Running, call.from)
        assertEquals(State.Enqueued, call.to)
        assertEquals(activeIds, call.excludedIds)

        sweeperJob.cancel()
    }

    @Test
    fun `when app transitions to foreground, a sweep is triggered`() = runTest {
        val repository = FakeOrphanTaskSweeperRepository()
        val activeIds = setOf(Uuid.random())
        val activeTaskTracker = FakeActiveTaskTracker(activeIds)
        val isBackground = MutableStateFlow(true)
        val appStateObserver = FakeAppStateObserver(isBackground)

        val sweeperJob = Job()
        OrphanTaskSweeper(
            repository = repository,
            scope = ApplicationScope(coroutineContext + sweeperJob),
            dispatchers = dispatchers(),
            activeTaskTracker = activeTaskTracker,
            appStateObserver = appStateObserver
        )

        runCurrent()
        assertEquals(1, repository.resetCalls.size) // Cold start sweep

        // Transition to foreground
        isBackground.value = false
        runCurrent()

        assertEquals(2, repository.resetCalls.size)
        val secondCall = repository.resetCalls[1]
        assertEquals(State.Running, secondCall.from)
        assertEquals(State.Enqueued, secondCall.to)
        assertEquals(activeIds, secondCall.excludedIds)

        sweeperJob.cancel()
    }

    @Test
    fun `when guard session starts, a background resurrection sweep is triggered`() = runTest {
        val repository = FakeOrphanTaskSweeperRepository()
        val activeIds = setOf(Uuid.random())
        val activeTaskTracker = FakeActiveTaskTracker(activeIds)
        val isBackground = MutableStateFlow(true)
        val appStateObserver = FakeAppStateObserver(isBackground)

        val sweeperJob = Job()
        val sweeper = OrphanTaskSweeper(
            repository = repository,
            scope = ApplicationScope(coroutineContext + sweeperJob),
            dispatchers = dispatchers(),
            activeTaskTracker = activeTaskTracker,
            appStateObserver = appStateObserver
        )

        runCurrent()
        assertEquals(1, repository.resetCalls.size) // Cold start sweep

        // Guard session starts (onStarted callback)
        sweeper.onStarted()
        runCurrent()

        assertEquals(2, repository.resetCalls.size)
        val secondCall = repository.resetCalls[1]
        assertEquals(State.Running, secondCall.from)
        assertEquals(State.Enqueued, secondCall.to)
        assertEquals(activeIds, secondCall.excludedIds)

        sweeperJob.cancel()
    }
}
