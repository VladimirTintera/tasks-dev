package eu.tintera.tasks.migrations

import eu.tintera.tasks.serialization.TaskDataSerializer

class MigrationBuilder(private val startVersion: Int, private val endVersion: Int) {

    @PublishedApi
    internal var inputMigrator: FieldMigrator<Any?, Any?>? = null

    @PublishedApi
    internal var outputMigrator: FieldMigrator<Any?, Any?>? = null

    @PublishedApi
    internal var progressMigrator: FieldMigrator<Any?, Any?>? = null

    inline fun <reified From, reified To> migrateInput(
        fromSerializer: TaskDataSerializer<From>,
        toSerializer: TaskDataSerializer<To>,
        noinline block: (From) -> To
    ) {
        @Suppress("UNCHECKED_CAST")
        inputMigrator = FieldMigrator(
            fromSerializer = fromSerializer,
            toSerializer = toSerializer,
            migrationBlock = block
        ) as FieldMigrator<Any?, Any?>
    }

    inline fun <reified From, reified To> migrateOutput(
        fromSerializer: TaskDataSerializer<From>,
        toSerializer: TaskDataSerializer<To>,
        noinline block: (From) -> To
    ) {
        @Suppress("UNCHECKED_CAST")
        outputMigrator = FieldMigrator(
            fromSerializer = fromSerializer,
            toSerializer = toSerializer,
            migrationBlock = block
        ) as FieldMigrator<Any?, Any?>
    }

    inline fun <reified From, reified To> migrateProgress(
        fromSerializer: TaskDataSerializer<From>,
        toSerializer: TaskDataSerializer<To>,
        noinline block: (From) -> To
    ) {
        @Suppress("UNCHECKED_CAST")
        progressMigrator = FieldMigrator(
            fromSerializer = fromSerializer,
            toSerializer = toSerializer,
            migrationBlock = block
        ) as FieldMigrator<Any?, Any?>
    }

    @PublishedApi
    internal fun build(): Migration {
        return Migration(
            startVersion = startVersion,
            endVersion = endVersion,
            inputMigrator = inputMigrator,
            outputMigrator = outputMigrator,
            progressMigrator = progressMigrator
        )
    }
}

inline fun migration(
    startVersion: Int,
    endVersion: Int,
    block: MigrationBuilder.() -> Unit
): Migration {
    // Vytvoříme builder, aplikujeme na něj uživatelův blok a rovnou ho zamkneme (build)
    return MigrationBuilder(startVersion, endVersion).apply(block).build()
}