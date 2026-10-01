package eu.tintera.background.tasks

import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * The WorkManager side of `SelfReplacingContinuationTest` (tasks/runtime, jvmTest): a running task
 * enqueues **its own** unique continuation with [ExistingTaskPolicy.Replace].
 *
 * WorkManager answers `REPLACE` by stopping the running worker and cancelling (and deleting) the rest
 * of the old chain. What must hold: the old head and its successors never run again, whatever the
 * old head returns, and the new chain runs exactly once.
 *
 * Runs against the sample application, which initializes the library on start.
 */
@RunWith(AndroidJUnit4::class)
class SelfReplacingContinuationDeviceTest {

    private enum class AfterReplace { RETURN_RETRY, WAIT }

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

    private class HeadHandler : InputTaskHandler<Input> {
        override suspend fun InputTaskScope<Input>.run(): TaskResult<Unit> {
            runs += "$HEAD:${data.chain}:${data.generation}"

            if (data.generation > 0) return TaskResult.success(Unit)

            Tasks.taskManager.enqueueChain(data.copy(generation = data.generation + 1))

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

    private class NextHandler : InputTaskHandler<Input> {
        override suspend fun InputTaskScope<Input>.run(): TaskResult<Unit> {
            runs += "$NEXT:${data.chain}:${data.generation}"
            return TaskResult.success(Unit)
        }
    }

    @Before
    fun setUp() {
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
    }

    @Test
    fun resultOfReplacedTaskIsDiscardedAndOnlyTheNewChainRuns() {
        assertSelfReplace(chain = "retry-${Uuid.random()}", afterReplace = AfterReplace.RETURN_RETRY)
    }

    @Test
    fun runningTaskThatReplacedItsOwnChainIsCancelledAndOnlyTheNewChainRuns() {
        val chain = "wait-${Uuid.random()}"
        assertSelfReplace(chain = chain, afterReplace = AfterReplace.WAIT)
        assertEquals(listOf(chain), cancelledHeads.filter { it == chain })
    }

    private fun assertSelfReplace(chain: String, afterReplace: AfterReplace) = runBlocking {
        val taskManager = Tasks.taskManager
        taskManager.enqueueChain(Input(chain = chain, generation = 0, afterReplace = afterReplace))

        val query = TaskInfoQueryBuilder().addTags(LabelTag(chain)).build()

        withTimeout(60.seconds) {
            taskManager.taskInfos(query).first { infos ->
                infos.any { it.identifier == NEXT && it.state == State.Succeeded }
            }
        }

        // Longer than the minimal WorkManager backoff (10 s): a retried old head would show up.
        delay(15.seconds)

        assertEquals(
            listOf("$HEAD:$chain:0", "$HEAD:$chain:1", "$NEXT:$chain:1"),
            runs.filter { it.split(":")[1] == chain },
        )

        // WorkManager deletes replaced work, so the old chain may be gone altogether; whatever is
        // left of it must be cancelled.
        val infos = taskManager.taskInfos(query).first()
        assertEquals(
            listOf(HEAD to State.Succeeded, NEXT to State.Succeeded),
            infos.filter { it.state != State.Cancelled }.map { it.identifier to it.state }.sortedBy { it.first },
        )
    }

    companion object {
        private const val HEAD = "test.SelfReplace.Head"
        private const val NEXT = "test.SelfReplace.Next"

        /** What ran, as `"<identifier>:<chain>:<generation>"`. */
        private val runs = ConcurrentLinkedQueue<String>()

        /** Heads that saw their own cancellation. */
        private val cancelledHeads = ConcurrentLinkedQueue<String>()

        private suspend fun TaskManager.enqueueChain(input: Input) = enqueueUniqueContinuation(
            uniqueName = input.chain,
            existingTaskPolicy = ExistingTaskPolicy.Replace,
            continuation = TaskContinuation(
                task = TaskRequest(identifier = HEAD, data = input, tags = setOf(LabelTag(input.chain))),
                next = TaskContinuation(
                    TaskRequest(identifier = NEXT, data = input, tags = setOf(LabelTag(input.chain)))
                ),
            ),
        )
    }
}
