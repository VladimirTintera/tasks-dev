package eu.tintera.background.tasks

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

private class FakeTaskScope<T : Any>(override val data: T) : TaskScope<T, Unit> {
    override val taskId: Uuid = Uuid.random()
    override val retryCount: Int = 0
    override val parents: List<ParentData> = emptyList()
    override val tags: Set<Tag> = emptySet()
    override suspend fun setProgress(data: Unit) = Unit
    override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo) = false
}

class InputTaskHandlerTest {

    /**
     * The engine calls a handler through `TaskScope<Input, Progress>.run()`, never through the
     * convenience overload directly. For an [InputTaskHandler] that path used to recurse into
     * itself: inside `with(handler) { run() }` the implicit receiver is a `TaskScope<T, Unit>`, so
     * overload resolution preferred `TaskScope<T, Unit>.run()` — the very function being executed —
     * over `InputTaskScope<T>.run()`. Every input handler therefore blew the stack on its first run.
     */
    @Test
    fun `handler runs through the TaskScope entry point instead of recursing`() = runTest {
        val handler = InputTaskHandler<String> { TaskResult.success(Unit) }

        val scope: TaskScope<String, Unit> = FakeTaskScope("payload")
        val result = with(handler) { scope.run() }

        assertEquals(TaskResult.success(Unit), result)
    }

    @Test
    fun `handler sees its typed input`() = runTest {
        var seen: String? = null
        val handler = InputTaskHandler<String> {
            seen = data
            TaskResult.success(Unit)
        }

        val scope: TaskScope<String, Unit> = FakeTaskScope("payload")
        with(handler) { scope.run() }

        assertEquals("payload", seen)
    }
}
