package eu.tintera.tasks.core.cleanup

import kotlin.time.Duration

interface DatabaseCleanupPolicy {
    val deleteGhostTasks: Boolean
    val ghostTaskTimeout: Duration?

    companion object {
        val DISABLED_GHOST_TASKS_POLICY : DatabaseCleanupPolicy = object : DatabaseCleanupPolicy {
            override val deleteGhostTasks: Boolean = false
            override val ghostTaskTimeout: Duration? = null
        }
    }
}