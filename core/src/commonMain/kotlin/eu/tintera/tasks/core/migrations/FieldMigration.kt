package eu.tintera.tasks.core.migrations

import kotlinx.serialization.KSerializer

class FieldMigrator<From, To>(
    val fromSerializer: KSerializer<From>,
    val toSerializer: KSerializer<To>,
    val migrationBlock: (From) -> To
) {
    // Tuto metodu zavolá až TaskEvaluator, který má přístup k SerializationEngine

}