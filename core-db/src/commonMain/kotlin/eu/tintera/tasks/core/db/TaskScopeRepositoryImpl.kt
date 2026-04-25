package eu.tintera.tasks.core.db

import eu.tintera.tasks.core.data.TaskScopeRepository
import eu.tintera.tasks.db.dao.TaskScopeDao
import kotlin.uuid.Uuid

internal class TaskScopeRepositoryImpl(
    private val taskScopeDao: TaskScopeDao,
) : TaskScopeRepository {

    override suspend fun updateProgressData(
        id: Uuid,
        progressData: ByteArray?
    ) = taskScopeDao.updateProgressData(id, progressData)
}