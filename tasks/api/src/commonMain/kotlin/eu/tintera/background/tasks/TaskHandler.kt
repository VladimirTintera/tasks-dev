package eu.tintera.background.tasks

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
        // The receiver is narrowed to InputTaskScope on purpose. Both `run()` overloads accept a
        // TaskScope — it is an InputTaskScope — and TaskScope<T, Unit>.run() is the more specific
        // of the two, so calling `run()` on the unnarrowed receiver resolves back to *this*
        // function and recurses until the stack is gone. Nothing in the signatures hints at that,
        // which is why it survived until an input handler was actually run.
        val input: InputTaskScope<T> = this
        return with(this@InputTaskHandler) { input.run() }
    }
}