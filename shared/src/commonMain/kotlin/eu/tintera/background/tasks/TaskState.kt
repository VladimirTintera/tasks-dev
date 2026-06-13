package eu.tintera.background.tasks

data class TaskState(
    val finished: List<TaskInfo>,
    val ongoing: List<TaskInfo>
)