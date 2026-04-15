package eu.tintera.tasks.migrations

import kotlinx.serialization.KSerializer

class FieldMigrator<From, To>(
    val fromSerializer: KSerializer<From>,
    val toSerializer: KSerializer<To>,
    val migrationBlock: (From) -> To
)