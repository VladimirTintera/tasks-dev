package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.ExistingTaskPolicy
import eu.tintera.background.tasks.InputTaskHandler
import eu.tintera.background.tasks.InputTaskScope
import eu.tintera.background.tasks.State
import eu.tintera.background.tasks.TaskContinuation
import eu.tintera.background.tasks.TaskInfo
import eu.tintera.background.tasks.TaskInfoQueryBuilder
import eu.tintera.background.tasks.TaskManager
import eu.tintera.background.tasks.TaskManagerConfiguration
import eu.tintera.background.tasks.TaskRequest
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.Tasks
import eu.tintera.background.tasks.TasksInitializer
import eu.tintera.background.tasks.register
import eu.tintera.background.tasks.registering
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * A task that enqueues **its own** unique continuation with [ExistingTaskPolicy.Replace] while it is
 * running — the way a consumer restarts a chain to reset its backoff.
 *
 * The engine has to come out of that with exactly one live chain: the running task and the rest of
 * its old chain end up [State.Cancelled], whatever the running task returns, and the new chain runs
 * once, with its successors attached to the new head rather than the old one.
 */
class SelfReplacingContinuationTest {

    private enum class AfterReplace {
        /** Keep going as if nothing happened — the engine must throw the result away. */
        RETURN_RETRY,

        /** Stay running — the engine must cancel the task. */
        WAIT,
    }

    private data class Input(val chain: String, val generation: Int, val afterReplace: AfterReplace)

    private class InputSerializer : Serializer<Input> {
        override fun encodeToBytes(value: Input) =
            "${value.chain}|${value.generation}|${value.afterReplace.name}".encodeToByteArray()

        override fun decodeFromBytes(bytes: ByteArray) = bytes.decodeToString().split("|").let {
            Input(it[0], it[1].toInt(), AfterReplace.valueOf(it[2]))
        }
    }

    private object UnitSerializer : Serializer<Unit> {
        override fun encodeToBytes(value: Unit) = ByteArray(0)
        override fun decodeFromBytes(bytes: ByteArray) = Unit
    }

    private inner class HeadHandler : InputTaskHandler<Input> {
        override suspend fun InputTaskScope<Input>.run(): TaskResult<Unit> {
            runs += "$HEAD:${data.chain}:${data.generation}"

            if (data.generation > 0) return TaskResult.success(Unit)

            taskManager.enqueueChain(data.copy(generation = data.generation + 1))

            return when (data.afterReplace) {
                AfterReplace.RETURN_RETRY -> TaskResult.retry()
                AfterReplace.WAIT -> try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    cancelledHeads += data.chain
                    throw e
                }
            }
        }
    }

    private inner class NextHandler : InputTaskHandler<Input> {
        override suspend fun InputTaskScope<Input>.run(): TaskResult<Unit> {
            runs += "$NEXT:${data.chain}:${data.generation}"
            return TaskResult.success(Unit)
        }
    }

    private val taskManager: TaskManager get() = Tasks.taskManager

    @BeforeTest
    fun setUp() {
        if (initialized) return
        initialized = true

        Tasks.registry.registering {
            Tasks.registry.register<HeadHandler, Input, Unit, Unit>(
                identifier = HEAD,
                inputSerializer = InputSerializer(),
                outputSerializer = UnitSerializer,
                progressSerializer = UnitSerializer,
            ) { HeadHandler() }

            Tasks.registry.register<NextHandler, Input, Unit, Unit>(
                identifier = NEXT,
                inputSerializer = InputSerializer(),
                outputSerializer = UnitSerializer,
                progressSerializer = UnitSerializer,
            ) { NextHandler() }
        }

        TasksInitializer.initialize(
            TaskManagerConfiguration(
                databasePath = Files.createTempDirectory("tasks-self-replace").toString(),
                databaseName = "tasks.db",
            )
        )
    }

    @Test
    fun `result of a replaced task is discarded and only the new chain runs`() =
        assertSelfReplace(chain = "retry", afterReplace = AfterReplace.RETURN_RETRY)

    @Test
    fun `running task that replaced its own chain is cancelled and only the new chain runs`() {
        assertSelfReplace(chain = "wait", afterReplace = AfterReplace.WAIT)
        assertEquals(listOf("wait"), cancelledHeads.toList())
    }

    private fun assertSelfReplace(chain: String, afterReplace: AfterReplace) = runBlocking {
        taskManager.enqueueChain(Input(chain = chain, generation = 0, afterReplace = afterReplace))

        val infos = withTimeout(30.seconds) {
            taskManager.taskInfos(
                TaskInfoQueryBuilder().addUniqueNames(chain).build()
            ).first { infos ->
                infos.size == 4 && infos.all { it.state.isTerminal() }
            }
        }

        // Give a stray duplicate a chance to show up before counting runs.
        kotlinx.coroutines.delay(1.seconds)

        assertEquals(
            listOf("$HEAD:$chain:0", "$HEAD:$chain:1", "$NEXT:$chain:1"),
            runs.filter { it.split(":")[1] == chain },
        )
        assertEquals(
            mapOf(
                HEAD to listOf(State.Cancelled, State.Succeeded),
                NEXT to listOf(State.Cancelled, State.Succeeded),
            ),
            infos.groupBy(TaskInfo::identifier) { it.state }.mapValues { (_, states) -> states.sorted() },
        )
    }

    private suspend fun TaskManager.enqueueChain(input: Input) = enqueueUniqueContinuation(
        uniqueName = input.chain,
        existingTaskPolicy = ExistingTaskPolicy.Replace,
        continuation = TaskContinuation(
            task = TaskRequest(identifier = HEAD, data = input),
            next = TaskContinuation(TaskRequest(identifier = NEXT, data = input)),
        ),
    )

    private fun State.isTerminal() = this == State.Cancelled || this == State.Succeeded || this == State.Failed

    companion object {
        private const val HEAD = "test.SelfReplace.Head"
        private const val NEXT = "test.SelfReplace.Next"

        /**
         * [TasksInitializer] is process-wide and can be initialized only once — and JUnit creates
         * a new instance per test, so everything the handlers touch lives here.
         */
        private var initialized = false

        /** What ran, as `"<identifier>:<chain>:<generation>"`. */
        private val runs = ConcurrentLinkedQueue<String>()

        /** Heads that saw their own cancellation. */
        private val cancelledHeads = ConcurrentLinkedQueue<String>()
    }
}
