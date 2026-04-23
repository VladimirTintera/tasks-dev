package eu.tintera.tasks.migrations

import eu.tintera.tasks.serialization.Serializer

@DslMarker
annotation class MigrationDsl

@MigrationDsl
class MigrationBuilder(
    private val startVersion: Int,
    private val endVersion: Int
) {

    @PublishedApi
    internal var inputMigrator: Migrator<Any, Any>? = null

    @PublishedApi
    internal var outputMigrator: Migrator<Any, Any>? = null

    @PublishedApi
    internal var progressMigrator: Migrator<Any, Any>? = null

    fun <From : Any, To : Any> input(
        fromSerializer: Serializer<From>,
        block: (From) -> To
    ) {
        inputMigrator = migrator(fromSerializer, block)
    }

    fun <From : Any, To : Any> output(
        fromSerializer: Serializer<From>,
        block: (From) -> To
    ) {
        outputMigrator = migrator(fromSerializer, block)
    }

    fun <From : Any, To : Any> progress(
        fromSerializer: Serializer<From>,
        block: (From) -> To
    ) {
        progressMigrator = migrator(fromSerializer, block)
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <From : Any, To : Any> migrator(
        fromSerializer: Serializer<From>,
        block: (From) -> To
    ) = Migrator(
        fromSerializer = fromSerializer,
        migrationBlock = block
    ) as Migrator<Any, Any>

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
): Migration = MigrationBuilder(startVersion, endVersion).apply(block).build()

@MigrationDsl
class MigrationsBuilder {

    private var migrations = mutableListOf<Migration>()

    @PublishedApi
    internal fun add(migration: Migration) {
        migrations.add(migration)
    }

    fun build(): List<Migration> = migrations.toList()
}

inline fun MigrationsBuilder.migration(
    startVersion: Int,
    endVersion: Int,
    block: MigrationBuilder.() -> Unit
) {
    add(MigrationBuilder(startVersion, endVersion).apply(block).build())
}


fun migrations(
    block: MigrationsBuilder.() -> Unit
): List<Migration> = MigrationsBuilder().apply { block() }.build()