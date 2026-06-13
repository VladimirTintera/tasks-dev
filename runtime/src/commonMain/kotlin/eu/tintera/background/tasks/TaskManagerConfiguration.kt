package eu.tintera.background.tasks

import eu.tintera.background.guard.ExecutionEnvironment
import kotlin.time.Duration

expect class TaskManagerConfiguration {
    val executionEnvironment: ExecutionEnvironment?
    val executionContextReleaseDebounce: Duration
    val databaseName: String
}