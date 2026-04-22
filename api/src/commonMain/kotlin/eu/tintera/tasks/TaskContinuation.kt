package eu.tintera.tasks

class TaskContinuation(
    val tasks: List<TaskRequest<*>>,
    val next: TaskContinuation? = null
) {
    constructor(
        task: TaskRequest<*>,
        next: TaskContinuation? = null
    ) : this(listOf(task), next)
}

infix fun TaskRequest<*>.then(nextTask: TaskRequest<*>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTask))
}

infix fun TaskRequest<*>.then(nextTasks: List<TaskRequest<*>>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTasks))
}

infix fun List<TaskRequest<*>>.then(nextTask: TaskRequest<*>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTask))
}

infix fun TaskContinuation.then(nextTask: TaskRequest<*>): TaskContinuation {
    return this.append(TaskContinuation(nextTask))
}

infix fun TaskContinuation.then(nextTasks: List<TaskRequest<*>>): TaskContinuation {
    return this.append(TaskContinuation(nextTasks))
}

private fun TaskContinuation.append(tail: TaskContinuation): TaskContinuation {
    return if (this.next == null) {
        TaskContinuation(tasks = this.tasks, next = tail)
    } else {
        TaskContinuation(tasks = this.tasks, next = this.next.append(tail))
    }
}