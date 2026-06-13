package eu.tintera.background.tasks.migrations

import eu.tintera.background.tasks.serialization.Serializer

class Migrator<From: Any, To: Any>(
    val fromSerializer: Serializer<From>,
    val migrationBlock: (From) -> To
)