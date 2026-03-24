package eu.tintera.tasks.core.data

data class FullTask(
    val task: Task,
    val tags: Set<String>
)