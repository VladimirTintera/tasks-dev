package eu.tintera.tasks

class TaskContinuation(
    val tasks: List<TaskRequest>,
    val next: TaskContinuation? = null
) {
    constructor(
        task: TaskRequest,
        next: TaskContinuation? = null
    ) : this(listOf(task), next)
}