package eu.tintera.tasks

import eu.tintera.guard.ExecutionEnvironment
import kotlin.time.Duration

expect class TaskManagerConfiguration {
    val executionEnvironment: ExecutionEnvironment?
    val executionContextReleaseDebounce: Duration
    val databaseName: String
}