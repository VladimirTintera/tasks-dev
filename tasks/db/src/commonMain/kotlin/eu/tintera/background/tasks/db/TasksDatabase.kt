package eu.tintera.background.tasks.db

import androidx.room3.*
import eu.tintera.background.tasks.db.dao.CleanableTaskDao
import eu.tintera.background.tasks.db.dao.DispatchableTaskDao
import eu.tintera.background.tasks.db.dao.OrphanTaskDao
import eu.tintera.background.tasks.db.dao.ParentConstraintDao
import eu.tintera.background.tasks.db.dao.SchedulableTaskDao
import eu.tintera.background.tasks.db.dao.TaskDao
import eu.tintera.background.tasks.db.dao.TaskEvaluatorDao
import eu.tintera.background.tasks.db.dao.TaskProcessorDao
import eu.tintera.background.tasks.db.dao.TaskResultDao
import eu.tintera.background.tasks.db.dao.TaskScopeDao
import eu.tintera.background.tasks.db.entities.*

internal const val databaseFile = "eu.tintera.tasks.db"

@Suppress("KotlinNoActualForExpect")
internal expect object TasksDatabaseConstructor : RoomDatabaseConstructor<TasksDatabase> {
    override fun initialize(): TasksDatabase
}

@Database(
    entities = [
        TaskEntity::class,
        TaskParentTaskEntity::class,
        TaskTagEntity::class
    ],
    exportSchema = true,
    version = 1,
)
@ConstructedBy(TasksDatabaseConstructor::class)
@ColumnTypeConverters(TasksTypeConverters::class)
internal abstract class TasksDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskTagDao(): TaskTagDao
    abstract fun taskParentTaskDao(): TaskParentTaskDao

    abstract fun taskProgressDataDao(): TaskScopeDao
    abstract fun taskDataDao(): TaskEvaluatorDao
    abstract fun taskProcessorDao(): TaskProcessorDao
    abstract fun dispatchableTaskDao(): DispatchableTaskDao
    abstract fun cleanableTaskDao(): CleanableTaskDao
    abstract fun schedulableTaskDao(): SchedulableTaskDao
    abstract fun orphanTaskDao(): OrphanTaskDao
    abstract fun parentConstraintDao() : ParentConstraintDao
    abstract fun taskResultDao() : TaskResultDao
}
