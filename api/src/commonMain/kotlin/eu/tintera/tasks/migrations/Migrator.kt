package eu.tintera.tasks.migrations

import eu.tintera.tasks.serialization.Serializer

class Migrator<From: Any, To: Any>(
    val fromSerializer: Serializer<From>,
    val migrationBlock: (From) -> To
)