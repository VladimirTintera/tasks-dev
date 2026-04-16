package eu.tintera.tasks.migrations

import eu.tintera.tasks.serialization.TaskDataSerializer

class FieldMigrator<From, To>(
    val fromSerializer: TaskDataSerializer<From>,
    val toSerializer: TaskDataSerializer<To>,
    val migrationBlock: (From) -> To
)