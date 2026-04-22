package eu.tintera.tasks.core.data

import eu.tintera.tasks.TaskInfo
import eu.tintera.tasks.core.TaskRegistry
import eu.tintera.tasks.core.migrations.MigrationResult

data class FullTask(
    val task: Task,
    val tags: Set<String>
)

