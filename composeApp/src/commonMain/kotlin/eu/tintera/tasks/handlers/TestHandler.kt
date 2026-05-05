package eu.tintera.tasks.handlers

import eu.tintera.tasks.*
import eu.tintera.tasks.MainViewModel.Companion.DEFAULT_TAG
import eu.tintera.tasks.serialization.TagSerializer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

data class TestTypedTag(
    val number: Int
) : Tag {

    companion object {

        val serializer = object : TagSerializer<TestTypedTag> {
            override fun encodeToString(value: TestTypedTag) = value.number.toString()

            override fun decodeFromStringOrNull(
                value: String
            ): TestTypedTag? = value.toIntOrNull()?.let {
                TestTypedTag(it)
            }
        }
    }

}

@Serializable
data class TestHandlerProgress(
    val totalCount: Int,
    val progress: Int,
    val parents: List<String> = emptyList()
)

@Serializable
data class TestHandlerData(
    val count: Int = 0,
    val name: String = ""
)

class TestHandler : TaskHandler<TestHandlerData, TestHandlerData, TestHandlerProgress> {

    override suspend fun TaskScope<TestHandlerData, TestHandlerProgress>.run(): TaskResult<TestHandlerData> {
        return merge(
            _retryEventBus.filter { it == taskId }.map {
                TaskResult.retry()
            },
            _failedEventBus.filter { it == taskId }.map {
                TaskResult.failure()
            },
            normalRun()
        ).first()
    }

    private fun TaskScope<TestHandlerData, TestHandlerProgress>.normalRun() = flow {
        repeat(data.count) {
            setProgress(
                TestHandlerProgress(
                    totalCount = data.count,
                    progress = it + 1,
                    parents = parentOutputsOfType<TestHandlerData>().map { it.name }
                )
            )
            delay(1.seconds)
        }
        emit(TaskResult.success(data))
    }

    companion object {
        private val _retryEventBus =
            MutableSharedFlow<Uuid>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        private val _failedEventBus =
            MutableSharedFlow<Uuid>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        fun retry(id: Uuid) {
            _retryEventBus.tryEmit(id)
        }

        fun fail(id: Uuid) {
            _failedEventBus.tryEmit(id)
        }


    }
}

fun testTaskRequest(
    count: Int,
    name: String,
    initialDelay: Duration = Duration.ZERO
) = taskRequest<TestHandler, TestHandlerData>(
    data = TestHandlerData(
        count = count,
        name = name
    ),
    tags = tags {
        tag(DEFAULT_TAG)
        TestTypedTag(Random.nextInt(0, 1000)).let {
            tag(it)
            tag(it.copy(number = it.number + 1))
        }
    },
    constraints = Constraints(
        requiresDeviceIdle = false,
        requiresNetwork = false
    ),
    initialDelay = initialDelay
)

suspend fun TaskManager.scheduleTestHandler(
    count: Int,
    initialDelay: Duration
) = enqueueTask(
    testTaskRequest(
        count = count,
        name = "test",
        initialDelay = initialDelay
    )
)