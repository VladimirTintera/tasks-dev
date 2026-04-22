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

// 1. Jeden úkol pokračuje jedním úkolem (A pak B)
infix fun TaskRequest<*>.then(nextTask: TaskRequest<*>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTask))
}

// 2. Jeden úkol pokračuje paralelními úkoly (A pak [B, C])
infix fun TaskRequest<*>.then(nextTasks: List<TaskRequest<*>>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTasks))
}

// 3. Paralelní úkoly pokračují jedním úkolem ([A, B] pak C)
infix fun List<TaskRequest<*>>.then(nextTask: TaskRequest<*>): TaskContinuation {
    return TaskContinuation(this, TaskContinuation(nextTask))
}

// 4. Napojování na už existující řetězec (A pak B pak C)
infix fun TaskContinuation.then(nextTask: TaskRequest<*>): TaskContinuation {
    return this.append(TaskContinuation(nextTask))
}

// 5. Napojování paralelních úkolů na existující řetězec
infix fun TaskContinuation.then(nextTasks: List<TaskRequest<*>>): TaskContinuation {
    return this.append(TaskContinuation(nextTasks))
}

private fun TaskContinuation.append(tail: TaskContinuation): TaskContinuation {
    return if (this.next == null) {
        // Jsme na konci řetězce, napojíme nový ocas
        TaskContinuation(tasks = this.tasks, next = tail)
    } else {
        // Nejsme na konci, zkopírujeme aktuální uzel a rekurzivně pokračujeme
        TaskContinuation(tasks = this.tasks, next = this.next.append(tail))
    }
}