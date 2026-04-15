package eu.tintera.tasks.migrations

import kotlinx.serialization.serializer

class MigrationBuilder(private val startVersion: Int, private val endVersion: Int) {

    @PublishedApi
    internal var inputMigrator: FieldMigrator<Any?, Any?>? = null
    @PublishedApi
    internal var outputMigrator: FieldMigrator<Any?, Any?>? = null
    @PublishedApi
    internal var progressMigrator: FieldMigrator<Any?, Any?>? = null

    inline fun <reified From, reified To> migrateInput(noinline block: (From) -> To) {
        @Suppress("UNCHECKED_CAST")
        inputMigrator = FieldMigrator(
            fromSerializer = serializer<From>(),
            toSerializer = serializer<To>(),
            migrationBlock = block
        ) as FieldMigrator<Any?, Any?>
    }

    inline fun <reified From, reified To> migrateOutput(noinline block: (From) -> To) {
        @Suppress("UNCHECKED_CAST")
        outputMigrator = FieldMigrator(
            fromSerializer = serializer<From>(),
            toSerializer = serializer<To>(),
            migrationBlock = block
        ) as FieldMigrator<Any?, Any?>
    }

    inline fun <reified From, reified To> migrateProgress(noinline block: (From) -> To) {
        @Suppress("UNCHECKED_CAST")
        progressMigrator = FieldMigrator(
            fromSerializer = serializer<From>(),
            toSerializer = serializer<To>(),
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