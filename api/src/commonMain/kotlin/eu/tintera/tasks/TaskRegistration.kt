package eu.tintera.tasks

import eu.tintera.tasks.migrations.Migration
import eu.tintera.tasks.serialization.Serializer

class TaskRegistration<I : Any, O : Any, P : Any>(
    val identifier: String,
    val currentVersion: Int,
    val factory: () -> TaskHandler<I, O, P>,
    val inputSerializer: Serializer<I>,
    val outputSerializer: Serializer<O>,
    val progressSerializer: Serializer<P>,
    val migrations: List<Migration>
) {
    init {
        require(identifier.isNotBlank()) { "Task identifier cannot be blank." }
        require(currentVersion >= 1) { "Task version must be at least 1." }
    }
}