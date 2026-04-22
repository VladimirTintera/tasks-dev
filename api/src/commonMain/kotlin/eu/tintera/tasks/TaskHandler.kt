package eu.tintera.tasks

import kotlin.reflect.KClass

fun interface TaskHandler<Input : Any, Output : Any, Progress : Any> {
    suspend fun TaskScope<Input, Progress>.run(): TaskResult<Output>
}

fun interface SimpleTaskHandler : TaskHandler<Unit, Unit, Unit> {

    suspend fun run(): TaskResult<Unit>

    override suspend fun TaskScope<Unit, Unit>.run(): TaskResult<Unit> {
        return this@SimpleTaskHandler.run()
    }
}

fun interface InputTaskHandler<T : Any> : TaskHandler<T, Unit, Unit> {

    suspend fun InputTaskScope<T>.run(): TaskResult<Unit>

    override suspend fun TaskScope<T, Unit>.run(): TaskResult<Unit> {
        with(this@InputTaskHandler) {
            return run()
        }
    }
}

val KClass<out TaskHandler<*, *, *>>.fullName: String
    get() = qualifiedName
        ?: error("Anonymous class and lambda could not be registered by type. Use registration with identifier instead.")
