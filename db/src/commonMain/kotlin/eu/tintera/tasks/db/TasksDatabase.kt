package eu.tintera.tasks.db

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import eu.tintera.tasks.db.entities.*

internal const val databaseFile = "eu.tintera.tasks.db"

@Suppress("KotlinNoActualForExpect")
internal expect object TasksDatabaseConstructor : RoomDatabaseConstructor<TasksDatabase> {
    override fun initialize(): TasksDatabase
}

@Database(
    entities = [
        TaskEntity::class,
        TaskParentTask::class,
        TaskTag::class
    ],
    exportSchema = true,
    version = 10,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = AutoMigration4to5Spec::class),
        AutoMigration(from = 5, to = 6, spec = AutoMigration5to6Spec::class),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ]
)
@ConstructedBy(TasksDatabaseConstructor::class)
@TypeConverters(TasksTypeConverters::class)
internal abstract class TasksDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskTagDao(): TaskTagDao
    abstract fun taskParentTaskDao(): TaskParentTaskDao
}

@DeleteColumn(tableName = "Task", columnName = "requiresSystemKeepAlive")
class AutoMigration4to5Spec : AutoMigrationSpec

@RenameColumn(tableName = "Task", fromColumnName = "retriesCount", toColumnName = "runAttemptCount")
class AutoMigration5to6Spec : AutoMigrationSpec