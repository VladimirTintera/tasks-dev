package eu.tintera.tasks

data class TaskState(
    val finished: List<TaskInfo>,
    val ongoing: List<TaskInfo>
)